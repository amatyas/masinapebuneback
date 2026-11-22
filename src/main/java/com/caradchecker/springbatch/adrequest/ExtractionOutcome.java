package com.caradchecker.springbatch.adrequest;

import com.fasterxml.jackson.databind.JsonNode;

record ExtractionOutcome(ExtractionProvider provider, ExtractionStatus status, JsonNode data, String error, long durationMs) {

	static ExtractionOutcome ok(ExtractionProvider provider, JsonNode data, long durationMs) {
		return new ExtractionOutcome(provider, ExtractionStatus.OK, data, null, durationMs);
	}

	static ExtractionOutcome error(ExtractionProvider provider, String error, long durationMs) {
		return new ExtractionOutcome(provider, ExtractionStatus.ERROR, null, error, durationMs);
	}
}
