package com.caradchecker.springbatch.rest.api.adrequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.caradchecker.springbatch.adrequest.AdCheckRequest;
import com.caradchecker.springbatch.adrequest.AdExtraction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record AdRequestResponse(
		String requestId,
		String url,
		String status,
		LocalDateTime createdAt,
		Map<String, ExtractionResult> extractions) {

	static AdRequestResponse from(AdCheckRequest request, List<AdExtraction> extractions, ObjectMapper objectMapper) {
		Map<String, ExtractionResult> mapped = new LinkedHashMap<>();
		for (AdExtraction extraction : extractions) {
			JsonNode data = null;
			String error = extraction.getErrorMessage();
			if (extraction.getResultJson() != null && !extraction.getResultJson().isBlank()) {
				try {
					data = objectMapper.readTree(extraction.getResultJson());
				} catch (JsonProcessingException ex) {
					if (error == null) {
						error = "JSON stocké illisible";
					}
				}
			}
			mapped.put(extraction.getProvider().name().toLowerCase(),
					new ExtractionResult(extraction.getStatus().name(), extraction.getDurationMs(), data, error));
		}
		return new AdRequestResponse(
				request.getId(),
				request.getUrl(),
				request.getStatus().name(),
				request.getCreatedAt(),
				mapped);
	}
}
