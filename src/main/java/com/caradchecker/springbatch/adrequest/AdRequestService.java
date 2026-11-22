package com.caradchecker.springbatch.adrequest;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdRequestService {

	private static final int MAX_URL_LENGTH = 2048;
	private static final String VERDICT_STAGE = AnalysisStage.LLM5_AD_VERDICT.name();

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final AdCheckRequestRepository repository;
	private final AdExtractionRepository extractionRepository;
	private final AdExtractionService extractionService;

	public AdRequestService(AdCheckRequestRepository repository, AdExtractionRepository extractionRepository,
			AdExtractionService extractionService) {
		this.repository = repository;
		this.extractionRepository = extractionRepository;
		this.extractionService = extractionService;
	}

	public AdCheckRequest create(String requestId, String rawUrl, String rawLanguage, String rawCountry) {
		String url = normalizeUrl(rawUrl);
		String language = ResponseLanguage.parse(rawLanguage).code();
		String country = UserCountry.parse(rawCountry).countryName();
		Optional<AdExtraction> cached = findCachedVerdict(url, language, country);
		AdCheckRequest request = new AdCheckRequest(requestId, url, language, country);
		if (cached.isPresent()) {
			request.setStatus(AdCheckStatus.COMPLETED);
			AdCheckRequest saved = repository.save(request);
			AdExtraction previous = cached.get();
			for (AdExtraction stage : extractionRepository.findByRequestId(previous.getRequestId())) {
				extractionRepository.save(new AdExtraction(saved.getId(), stage.getProvider(), stage.getStage(),
						stage.getStatus(), stage.getResultJson(), stage.getErrorMessage(), 0));
			}
			log.info("{} cache hit url={} language={} country={} sourceRequest={}", requestId, url, language, country,
					previous.getRequestId());
			return saved;
		}
		AdCheckRequest saved = repository.save(request);
		extractionService.extract(saved.getId());
		return saved;
	}

	public List<AdExtraction> extractions(String requestId) {
		return extractionRepository.findByRequestId(requestId);
	}

	private Optional<AdExtraction> findCachedVerdict(String url, String language, String country) {
		List<AdCheckRequest> previous = repository.findByUrlAndLanguageAndCountryOrderByCreatedAtDesc(url, language, country);
		for (AdCheckRequest request : previous) {
			Optional<AdExtraction> verdict = extractionRepository.findFirstByRequestIdAndStageAndStatusOrderByCreatedAtDesc(
					request.getId(), VERDICT_STAGE, ExtractionStatus.OK);
			if (verdict.isPresent() && verdict.get().getResultJson() != null && !verdict.get().getResultJson().isBlank()) {
				return verdict;
			}
		}
		return Optional.empty();
	}

	public AdCheckRequest get(String requestId) {
		if (requestId == null || requestId.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'identifiant de requête est obligatoire");
		}
		return repository.findById(requestId.trim())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requête introuvable"));
	}

	private String normalizeUrl(String rawUrl) {
		if (rawUrl == null || rawUrl.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lien de l'annonce est obligatoire");
		}
		String url = rawUrl.trim();
		if (url.length() > MAX_URL_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lien de l'annonce est trop long");
		}
		URI uri;
		try {
			uri = URI.create(url);
		} catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lien de l'annonce est invalide");
		}
		String scheme = uri.getScheme();
		if (scheme == null || uri.getHost() == null
				|| !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lien doit être une URL http ou https");
		}
		return url;
	}
}
