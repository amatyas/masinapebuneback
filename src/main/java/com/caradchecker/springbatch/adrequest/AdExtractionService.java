package com.caradchecker.springbatch.adrequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.caradchecker.springbatch.llm.GeminiStructuredClient;
import com.caradchecker.springbatch.llm.ListingPageFetcher;
import com.caradchecker.springbatch.llm.LlmProperties;
import com.caradchecker.springbatch.llm.LlmStageCatalog;
import com.caradchecker.springbatch.llm.OpenAiStructuredClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AdExtractionService {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final AdCheckRequestRepository requestRepository;
	private final AdExtractionRepository extractionRepository;
	private final GeminiStructuredClient geminiClient;
	private final OpenAiStructuredClient openAiClient;
	private final ListingPageFetcher listingPageFetcher;
	private final LlmStageCatalog stageCatalog;
	private final LlmProperties properties;
	private final ObjectMapper objectMapper;
	private final Executor stageExecutor;
	private final AdRequestProgress progress;

	public AdExtractionService(AdCheckRequestRepository requestRepository, AdExtractionRepository extractionRepository,
			GeminiStructuredClient geminiClient, OpenAiStructuredClient openAiClient, ListingPageFetcher listingPageFetcher,
			LlmStageCatalog stageCatalog, LlmProperties properties, ObjectMapper objectMapper,
			@Qualifier("llmStageExecutor") Executor stageExecutor, AdRequestProgress progress) {
		this.requestRepository = requestRepository;
		this.extractionRepository = extractionRepository;
		this.geminiClient = geminiClient;
		this.openAiClient = openAiClient;
		this.listingPageFetcher = listingPageFetcher;
		this.stageCatalog = stageCatalog;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.stageExecutor = stageExecutor;
		this.progress = progress;
	}

	@Async("adExtractionExecutor")
	public void extract(String requestId) {
		long start = System.nanoTime();
		AdCheckRequest request = requestRepository.findById(requestId).orElse(null);
		if (request == null) {
			log.warn("{} incoming extraction ignored, request missing", requestId);
			return;
		}
		log.info("{} incoming extraction url={}", requestId, request.getUrl());
		try {
			updateStatus(requestId, AdCheckStatus.PROCESSING);
			ExtractionProvider provider = configuredProvider();
			ExtractionOutcome llm1 = runStage(requestId, provider, AnalysisStage.LLM1_AD_EXTRACTOR, () -> extractListing(requestId, provider, request));
			if (llm1.status() != ExtractionStatus.OK || llm1.data() == null || !hasListingData(llm1.data())) {
				updateStatus(requestId, AdCheckStatus.FAILED);
				return;
			}
			log.info("{} parallel llm2_mechanical_check llm3_market_price_check llm4_ad_check", requestId);
			CompletableFuture<ExtractionOutcome> llm2 = CompletableFuture.supplyAsync(
					() -> runStage(requestId, provider, AnalysisStage.LLM2_MECHANICAL_CHECK, () -> complete(requestId, provider, null, AnalysisStage.LLM2_MECHANICAL_CHECK, "analysis", request.getLanguage(), request.getCountry(), AnalysisInputs.llm2(objectMapper, llm1.data(), request.getCountry()), true, request.getUrl())),
					stageExecutor);
			CompletableFuture<ExtractionOutcome> llm3 = CompletableFuture.supplyAsync(
					() -> runStage(requestId, provider, AnalysisStage.LLM3_MARKET_PRICE_CHECK, () -> complete(requestId, provider, null, AnalysisStage.LLM3_MARKET_PRICE_CHECK, "analysis", request.getLanguage(), request.getCountry(), AnalysisInputs.llm3(objectMapper, llm1.data(), request.getCountry()), true, request.getUrl())),
					stageExecutor);
			CompletableFuture<ExtractionOutcome> llm4 = CompletableFuture.supplyAsync(
					() -> runStage(requestId, provider, AnalysisStage.LLM4_AD_CHECK, () -> complete(requestId, provider, null, AnalysisStage.LLM4_AD_CHECK, "analysis", request.getLanguage(), null, AnalysisInputs.llm4(objectMapper, llm1.data(), request.getUrl()), false, request.getUrl())),
					stageExecutor);
			CompletableFuture.allOf(llm2, llm3, llm4).join();
			ExtractionOutcome technical = llm2.join();
			ExtractionOutcome market = llm3.join();
			ExtractionOutcome audit = llm4.join();
			ExtractionOutcome synthesis = runStage(requestId, provider, AnalysisStage.LLM5_AD_VERDICT,
					() -> complete(requestId, provider, null, AnalysisStage.LLM5_AD_VERDICT, "analysis", request.getLanguage(), null, AnalysisInputs.llm5(objectMapper, llm1.data(), technical, market, audit), false, request.getUrl()));
			updateStatus(requestId, synthesis.status() == ExtractionStatus.OK ? AdCheckStatus.COMPLETED : AdCheckStatus.FAILED);
		} catch (RuntimeException ex) {
			log.error("{} extraction failed", requestId, ex);
			updateStatus(requestId, AdCheckStatus.FAILED);
		} finally {
			log.info("{} extraction total execution time {} ms", requestId, elapsedMillis(start));
		}
	}

	private JsonNode extractListing(String requestId, ExtractionProvider provider, AdCheckRequest request) {
		String url = request.getUrl();
		String language = request.getLanguage();
		ExtractionProvider listingProvider = llm1Provider(provider);
		String listingModel = properties.getAdExtractorModel();
		try {
			JsonNode opened = complete(requestId, listingProvider, listingModel, AnalysisStage.LLM1_AD_EXTRACTOR, "url", language, null, AnalysisInputs.llm1FromUrl(url), true, url);
			assertLlm1Extraction(opened);
			if (hasListingData(opened)) {
				return opened;
			}
			log.info("{} preloading page, llm returned no listing fields", requestId);
		} catch (RuntimeException ex) {
			if (isConfigurationError(ex)) {
				throw ex;
			}
			log.info("{} preloading page after llm1_ad_extractor failure: {}", requestId, ex.getMessage());
		}
		String page = listingPageFetcher.fetch(url);
		JsonNode extracted = complete(requestId, listingProvider, listingModel, AnalysisStage.LLM1_AD_EXTRACTOR, "page", language, null, AnalysisInputs.llm1FromPage(url, page), true, url);
		assertLlm1Extraction(extracted);
		return extracted;
	}

	private void assertLlm1Extraction(JsonNode data) {
		if (data != null && data.path("vehicle").isObject() && !data.has("executive_summary") && !data.has("final_verdict")) {
			return;
		}
		throw new IllegalStateException("llm1_ad_extractor a renvoyé une synthèse au lieu de l'extraction");
	}

	private JsonNode complete(String requestId, ExtractionProvider provider, String model, AnalysisStage stage, String attempt, String language, String country, Object userPayload, boolean openUrl, String url) {
		long start = System.nanoTime();
		String schemaName = stageCatalog.schemaName(stage);
		log.info("{} stage={} attempt={} provider={} model={} language={} schema={} incoming llm call", requestId, stage.name().toLowerCase(), attempt, provider.name().toLowerCase(), model == null ? "" : model, language, schemaName);
		try {
			String userPrompt = userPayload instanceof String text ? text : write(userPayload);
			String systemPrompt = stageCatalog.systemPrompt(stage) + "\n\n" + ResponseLanguage.parse(language).instruction();
			if (country != null && !country.isBlank()) {
				UserCountry userCountry = UserCountry.parse(country);
				String countryInstruction = stage == AnalysisStage.LLM3_MARKET_PRICE_CHECK
						? userCountry.marketInstruction()
						: userCountry.instruction();
				systemPrompt = systemPrompt + "\n\n" + countryInstruction;
			}
			return switch (provider) {
				case GEMINI -> geminiClient.complete(systemPrompt, userPrompt, stageCatalog.geminiSchema(stage), openUrl, model);
				case OPENAI -> openAiClient.complete(systemPrompt, userPrompt, stageCatalog.openAiFormat(stage), openUrl, url, model);
			};
		} finally {
			log.info("{} stage={} attempt={} provider={} model={} language={} schema={} execution time {} ms", requestId, stage.name().toLowerCase(), attempt, provider.name().toLowerCase(), model == null ? "" : model, language, schemaName, elapsedMillis(start));
		}
	}

	private ExtractionProvider llm1Provider(ExtractionProvider fallback) {
		String configured = properties.getAdExtractorProvider();
		if (configured == null || configured.isBlank()) {
			return fallback;
		}
		try {
			return ExtractionProvider.valueOf(configured.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new IllegalStateException("Fournisseur ad-extractor inconnu: " + configured.trim());
		}
	}

	private String write(Object value) {
		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Entrée LLM illisible");
		}
	}

	private ExtractionProvider configuredProvider() {
		String value = properties.getProvider();
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Fournisseur LLM absent (llm.provider)");
		}
		try {
			return ExtractionProvider.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new IllegalStateException("Fournisseur LLM inconnu: " + value.trim());
		}
	}

	private boolean hasListingData(JsonNode data) {
		if (data == null || !data.isObject()) {
			return false;
		}
		JsonNode vehicle = data.path("vehicle");
		return present(vehicle.path("listing").path("title"))
				|| present(vehicle.path("listing").path("advertised_price"))
				|| present(vehicle.path("identity").path("make"))
				|| present(vehicle.path("identity").path("model"))
				|| present(vehicle.path("usage").path("odometer_km"));
	}

	private boolean present(JsonNode value) {
		if (value == null || value.isNull() || value.isMissingNode()) {
			return false;
		}
		if (value.isTextual()) {
			return !value.asText().isBlank();
		}
		return value.isNumber() || value.isBoolean();
	}

	private boolean isConfigurationError(RuntimeException ex) {
		return ex.getMessage() != null && ex.getMessage().contains("Clé API");
	}

	private ExtractionOutcome runStage(String requestId, ExtractionProvider provider, AnalysisStage stage, Supplier<JsonNode> supplier) {
		ExtractionOutcome outcome = call(requestId, provider, stage, supplier);
		save(requestId, outcome);
		return outcome;
	}

	private ExtractionOutcome call(String requestId, ExtractionProvider provider, AnalysisStage stage, Supplier<JsonNode> supplier) {
		long start = System.nanoTime();
		try {
			JsonNode data = supplier.get();
			long duration = elapsedMillis(start);
			log.info("{} stage={} status=OK durationMs={}", requestId, stage.name().toLowerCase(), duration);
			log.info("{} stage={} extracted\n{}", requestId, stage.name().toLowerCase(), extractedText(data));
			return ExtractionOutcome.ok(provider, stage, data, duration);
		} catch (RuntimeException ex) {
			long duration = elapsedMillis(start);
			String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
			log.warn("{} stage={} status=ERROR durationMs={} error={}", requestId, stage.name().toLowerCase(), duration, message);
			return ExtractionOutcome.error(provider, stage, truncate(message), duration);
		}
	}

	private String extractedText(JsonNode data) {
		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
		} catch (JsonProcessingException ex) {
			return data.toString();
		}
	}

	private void save(String requestId, ExtractionOutcome outcome) {
		String json = null;
		String error = outcome.error();
		ExtractionStatus status = outcome.status();
		if (outcome.data() != null) {
			try {
				json = objectMapper.writeValueAsString(outcome.data());
			} catch (JsonProcessingException ex) {
				status = ExtractionStatus.ERROR;
				error = "Sérialisation impossible";
			}
		}
		extractionRepository.save(new AdExtraction(requestId, outcome.provider(), outcome.stage().name(), status, json, error, outcome.durationMs()));
		progress.changed(requestId);
	}

	private void updateStatus(String requestId, AdCheckStatus status) {
		requestRepository.findById(requestId).ifPresent(request -> {
			request.setStatus(status);
			requestRepository.save(request);
			progress.changed(requestId);
		});
	}

	private long elapsedMillis(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000L;
	}

	private String truncate(String message) {
		String cleaned = message.replaceAll("\\s+", " ").trim();
		return cleaned.length() <= 2000 ? cleaned : cleaned.substring(0, 2000);
	}
}
