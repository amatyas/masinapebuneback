package com.caradchecker.springbatch.rest.api.adrequest;

import com.fasterxml.jackson.databind.JsonNode;

public record ExtractionResult(String status, Long durationMs, JsonNode data, String error) {
}
