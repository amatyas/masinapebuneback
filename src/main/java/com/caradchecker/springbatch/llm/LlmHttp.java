package com.caradchecker.springbatch.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

final class LlmHttp {

	private LlmHttp() {
	}

	static String post(HttpClient httpClient, URI uri, Map<String, String> headers, String json) {
		HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofSeconds(180))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
		headers.forEach(builder::header);
		try {
			HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() / 100 != 2) {
				throw new IllegalStateException("HTTP " + response.statusCode() + " " + abbreviate(response.body()));
			}
			return response.body();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Appel interrompu");
		} catch (IOException ex) {
			throw new IllegalStateException("Appel impossible: " + ex.getMessage());
		}
	}

	static String abbreviate(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		String cleaned = value.replaceAll("\\s+", " ").trim();
		return cleaned.length() <= 500 ? cleaned : cleaned.substring(0, 500);
	}
}
