package com.caradchecker.springbatch.adrequest;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.caradchecker.springbatch.llm.GeminiStructuredClient;
import com.caradchecker.springbatch.llm.ListingPageFetcher;
import com.caradchecker.springbatch.llm.LlmProperties;
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
	private final LlmProperties properties;
	private final ObjectMapper objectMapper;

	public AdExtractionService(AdCheckRequestRepository requestRepository, AdExtractionRepository extractionRepository,
			GeminiStructuredClient geminiClient, OpenAiStructuredClient openAiClient, ListingPageFetcher listingPageFetcher,
			LlmProperties properties, ObjectMapper objectMapper) {
		this.requestRepository = requestRepository;
		this.extractionRepository = extractionRepository;
		this.geminiClient = geminiClient;
		this.openAiClient = openAiClient;
		this.listingPageFetcher = listingPageFetcher;
		this.properties = properties;
		this.objectMapper = objectMapper;
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
			ExtractionOutcome outcome = call(requestId, provider, () -> extractWith(requestId, provider, request.getUrl()));
			log.info("{} extracted\n{}", requestId, extractedText(outcome));
			extractionRepository.save(toEntity(requestId, outcome));
			updateStatus(requestId, outcome.status() == ExtractionStatus.OK ? AdCheckStatus.COMPLETED : AdCheckStatus.FAILED);
		} catch (RuntimeException ex) {
			log.error("{} extraction failed", requestId, ex);
			updateStatus(requestId, AdCheckStatus.FAILED);
		} finally {
			log.info("{} extraction total execution time {} ms", requestId, elapsedMillis(start));
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

	private JsonNode extractWith(String requestId, ExtractionProvider provider, String url) {
		JsonNode opened = null;
		try {
			opened = extractFromLink(provider, url);
		} catch (RuntimeException ex) {
			if (isConfigurationError(ex)) {
				throw ex;
			}
			log.info("{} preloading page after llm error: {}", requestId, ex.getMessage());
			return extractFromPage(provider, url);
		}
		if (hasListingData(opened)) {
			return opened;
		}
		log.info("{} preloading page, llm returned no listing fields", requestId);
		return extractFromPage(provider, url);
	}

	private JsonNode extractFromLink(ExtractionProvider provider, String url) {
		return switch (provider) {
			case GEMINI -> geminiClient.extractFromLink(url);
			case OPENAI -> openAiClient.extractFromLink(url);
		};
	}

	private JsonNode extractFromPage(ExtractionProvider provider, String url) {
		String pageContent = listingPageFetcher.fetch(url);
		return switch (provider) {
			case GEMINI -> geminiClient.extractFromPage(url, pageContent);
			case OPENAI -> openAiClient.extractFromPage(url, pageContent);
		};
	}

	private boolean hasListingData(JsonNode data) {
		if (data == null || !data.isObject()) {
			return false;
		}
		JsonNode vehicle = data.path("vehicle");
		return present(vehicle.path("listing").path("title"))
				|| present(vehicle.path("listing").path("advertised_price"))
				|| present(vehicle.path("identification").path("make"))
				|| present(vehicle.path("identification").path("model"))
				|| present(vehicle.path("usage").path("odometer_km"));
	}

	private boolean present(JsonNode value) {
		if (value == null || value.isNull() || value.isMissingNode()) {
			return false;
		}
		if (value.isTextual()) {
			return !value.asText().isBlank();
		}
		return value.isNumber() || value.isBoolean() || (value.isContainerNode() && !value.isEmpty());
	}

	private boolean isConfigurationError(RuntimeException ex) {
		return ex.getMessage() != null && ex.getMessage().contains("Clé API");
	}

	private ExtractionOutcome call(String requestId, ExtractionProvider provider, Supplier<JsonNode> supplier) {
		long start = System.nanoTime();
		try {
			JsonNode data = supplier.get();
			long duration = elapsedMillis(start);
			log.info("{} provider={} status=OK durationMs={}", requestId, provider.name().toLowerCase(), duration);
			return ExtractionOutcome.ok(provider, data, duration);
		} catch (RuntimeException ex) {
			long duration = elapsedMillis(start);
			String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
			log.warn("{} provider={} status=ERROR durationMs={} error={}", requestId, provider.name().toLowerCase(), duration, message);
			return ExtractionOutcome.error(provider, truncate(message), duration);
		}
	}

	private String extractedText(ExtractionOutcome outcome) {
		if (outcome.data() == null) {
			return outcome.error();
		}
		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(outcome.data());
		} catch (JsonProcessingException ex) {
			return outcome.data().toString();
		}
	}

	private AdExtraction toEntity(String requestId, ExtractionOutcome outcome) {
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
		return new AdExtraction(requestId, outcome.provider(), status, json, error, outcome.durationMs());
	}

	private void updateStatus(String requestId, AdCheckStatus status) {
		requestRepository.findById(requestId).ifPresent(request -> {
			request.setStatus(status);
			requestRepository.save(request);
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
