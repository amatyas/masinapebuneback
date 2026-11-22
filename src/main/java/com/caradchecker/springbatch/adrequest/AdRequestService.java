package com.caradchecker.springbatch.adrequest;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdRequestService {

	private static final int MAX_URL_LENGTH = 2048;

	private final AdCheckRequestRepository repository;
	private final AdExtractionRepository extractionRepository;
	private final AdExtractionService extractionService;

	public AdRequestService(AdCheckRequestRepository repository, AdExtractionRepository extractionRepository,
			AdExtractionService extractionService) {
		this.repository = repository;
		this.extractionRepository = extractionRepository;
		this.extractionService = extractionService;
	}

	public AdCheckRequest create(String requestId, String rawUrl) {
		String url = normalizeUrl(rawUrl);
		AdCheckRequest saved = repository.save(new AdCheckRequest(requestId, url));
		extractionService.extract(saved.getId());
		return saved;
	}

	public List<AdExtraction> extractions(String requestId) {
		return extractionRepository.findByRequestId(requestId);
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
