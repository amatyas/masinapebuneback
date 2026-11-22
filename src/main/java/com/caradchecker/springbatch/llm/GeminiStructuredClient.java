package com.caradchecker.springbatch.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class GeminiStructuredClient {

	private final LlmProperties properties;
	private final CarAdSchema schema;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	public GeminiStructuredClient(LlmProperties properties, CarAdSchema schema, ObjectMapper objectMapper, HttpClient llmHttpClient) {
		this.properties = properties;
		this.schema = schema;
		this.objectMapper = objectMapper;
		this.httpClient = llmHttpClient;
	}

	public JsonNode extractFromLink(String url) {
		return generate(buildLinkBody(url));
	}

	public JsonNode extractFromPage(String url, String pageContent) {
		return generate(buildPageBody(url, pageContent));
	}

	private JsonNode generate(String body) {
		LlmProperties.Provider provider = properties.getGemini();
		if (!provider.hasApiKey()) {
			throw new IllegalStateException("Clé API Gemini absente (GEMINI_API_KEY)");
		}
		String model = blankToDefault(provider.getModel(), "gemini-3.5-flash");
		String base = blankToDefault(provider.getUrl(), "https://generativelanguage.googleapis.com/v1beta");
		if (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		String response = LlmHttp.post(
				httpClient,
				URI.create(base + "/models/" + model + ":generateContent"),
				Map.of("x-goog-api-key", provider.getApiKey()),
				body);
		return readStructuredJson(response);
	}

	private String buildLinkBody(String url) {
		ObjectNode body = baseBody(CarAdSchema.promptForUrl(url));
		body.putArray("tools").addObject().putObject("urlContext");
		return write(body);
	}

	private String buildPageBody(String url, String pageContent) {
		return write(baseBody(CarAdSchema.prompt(url, pageContent)));
	}

	private ObjectNode baseBody(String prompt) {
		ObjectNode body = objectMapper.createObjectNode();
		ArrayNode contents = body.putArray("contents");
		ArrayNode parts = contents.addObject().putArray("parts");
		parts.addObject().put("text", prompt);
		ObjectNode text = body.putObject("generationConfig").putObject("responseFormat").putObject("text");
		text.put("mimeType", "APPLICATION_JSON");
		text.set("schema", schema.copy());
		return body;
	}

	private String write(ObjectNode body) {
		try {
			return objectMapper.writeValueAsString(body);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Requête Gemini invalide");
		}
	}

	private JsonNode readStructuredJson(String responseBody) {
		JsonNode root;
		try {
			root = objectMapper.readTree(responseBody);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Réponse Gemini non JSON: " + LlmHttp.abbreviate(responseBody));
		}
		if (root.hasNonNull("error")) {
			throw new IllegalStateException(root.path("error").path("message").asText("erreur Gemini"));
		}
		String text = textParts(root);
		if (text.isBlank()) {
			String reason = root.path("promptFeedback").path("blockReason").asText("");
			if (reason.isBlank()) {
				reason = root.path("candidates").path(0).path("finishReason").asText("réponse vide");
			}
			throw new IllegalStateException(reason);
		}
		return parseJson(text);
	}

	private String textParts(JsonNode root) {
		JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
		StringBuilder text = new StringBuilder();
		if (parts.isArray()) {
			for (JsonNode part : parts) {
				if (part.path("thought").asBoolean(false)) {
					continue;
				}
				JsonNode value = part.get("text");
				if (value != null && !value.isNull()) {
					text.append(value.asText());
				}
			}
		}
		return text.toString().trim();
	}

	private JsonNode parseJson(String text) {
		String trimmed = stripFence(text);
		try {
			return objectMapper.readTree(trimmed);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("JSON Gemini illisible: " + LlmHttp.abbreviate(trimmed));
		}
	}

	private String stripFence(String text) {
		String trimmed = text.trim();
		if (!trimmed.startsWith("```")) {
			return trimmed;
		}
		int firstLine = trimmed.indexOf('\n');
		int lastFence = trimmed.lastIndexOf("```");
		if (firstLine >= 0 && lastFence > firstLine) {
			return trimmed.substring(firstLine + 1, lastFence).trim();
		}
		return trimmed;
	}

	private String blankToDefault(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}
}
