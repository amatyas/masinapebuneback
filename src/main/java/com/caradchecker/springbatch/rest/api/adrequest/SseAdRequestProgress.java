package com.caradchecker.springbatch.rest.api.adrequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.caradchecker.springbatch.adrequest.AdCheckRequestRepository;
import com.caradchecker.springbatch.adrequest.AdExtractionRepository;
import com.caradchecker.springbatch.adrequest.AdRequestProgress;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SseAdRequestProgress implements AdRequestProgress {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final AdCheckRequestRepository requestRepository;
	private final AdExtractionRepository extractionRepository;
	private final AdRequestEvents events;
	private final ObjectMapper objectMapper;

	public SseAdRequestProgress(AdCheckRequestRepository requestRepository, AdExtractionRepository extractionRepository,
			AdRequestEvents events, ObjectMapper objectMapper) {
		this.requestRepository = requestRepository;
		this.extractionRepository = extractionRepository;
		this.events = events;
		this.objectMapper = objectMapper;
	}

	@Override
	public void changed(String requestId) {
		try {
			events.publish(requestId, () -> snapshot(requestId));
		} catch (RuntimeException ex) {
			log.warn("{} progress notification failed: {}", requestId, ex.getMessage());
		}
	}

	private AdRequestResponse snapshot(String requestId) {
		return requestRepository.findById(requestId)
				.map(request -> AdRequestResponse.from(request, extractionRepository.findByRequestId(requestId), objectMapper))
				.orElse(null);
	}
}
