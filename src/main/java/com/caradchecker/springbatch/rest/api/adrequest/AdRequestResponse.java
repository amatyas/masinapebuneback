package com.caradchecker.springbatch.rest.api.adrequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.caradchecker.springbatch.adrequest.AdCheckRequest;
import com.caradchecker.springbatch.adrequest.AdExtraction;
import com.caradchecker.springbatch.adrequest.AnalysisStage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record AdRequestResponse(
		String requestId,
		String url,
		String language,
		String country,
		String status,
		LocalDateTime createdAt,
		Map<String, ExtractionResult> extractions) {

	static AdRequestResponse from(AdCheckRequest request, List<AdExtraction> extractions, ObjectMapper objectMapper) {
		List<AdExtraction> ordered = new ArrayList<>(extractions);
		ordered.sort(Comparator.comparingInt(extraction -> stageRank(extraction.getStage())));
		Map<String, ExtractionResult> mapped = new LinkedHashMap<>();
		for (AdExtraction extraction : ordered) {
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
			mapped.put(extraction.getStage() == null ? extraction.getProvider().name().toLowerCase() : extraction.getStage().toLowerCase(),
					new ExtractionResult(extraction.getStatus().name(), extraction.getDurationMs(), data, error));
		}
		return new AdRequestResponse(
				request.getId(),
				request.getUrl(),
				request.getLanguage(),
				request.getCountry(),
				request.getStatus().name(),
				request.getCreatedAt(),
				mapped);
	}

	private static int stageRank(String stage) {
		if (stage == null || stage.isBlank()) {
			return Integer.MAX_VALUE;
		}
		try {
			return AnalysisStage.valueOf(stage).ordinal();
		} catch (IllegalArgumentException ex) {
			return Integer.MAX_VALUE;
		}
	}
}
