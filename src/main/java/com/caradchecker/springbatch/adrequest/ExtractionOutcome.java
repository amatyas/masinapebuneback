package com.caradchecker.springbatch.adrequest;

import com.fasterxml.jackson.databind.JsonNode;

record ExtractionOutcome(ExtractionProvider provider, AnalysisStage stage, ExtractionStatus status, JsonNode data, String error, long durationMs) {

	static ExtractionOutcome ok(ExtractionProvider provider, AnalysisStage stage, JsonNode data, long durationMs) {
		return new ExtractionOutcome(provider, stage, ExtractionStatus.OK, data, null, durationMs);
	}

	static ExtractionOutcome error(ExtractionProvider provider, AnalysisStage stage, String error, long durationMs) {
		return new ExtractionOutcome(provider, stage, ExtractionStatus.ERROR, null, error, durationMs);
	}
}
