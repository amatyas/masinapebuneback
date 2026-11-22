package com.caradchecker.springbatch.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class OpenAiStructuredClient {

	private final LlmProperties properties;
	private final CarAdSchema schema;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	public OpenAiStructuredClient(LlmProperties properties, CarAdSchema schema, ObjectMapper objectMapper, HttpClient llmHttpClient) {
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
		LlmProperties.Provider provider = properties.getOpenai();
		if (!provider.hasApiKey()) {
			throw new IllegalStateException("Clé API OpenAI absente (OPENAI_API_KEY)");
		}
		String endpoint = blankToDefault(provider.getUrl(), "https://api.openai.com/v1/responses");
		String response = LlmHttp.post(
				httpClient,
				URI.create(endpoint),
				Map.of("Authorization", "Bearer " + provider.getApiKey()),
				body);
		return readStructuredJson(response);
	}

	private String buildLinkBody(String url) {
		ObjectNode body = baseBody(CarAdSchema.promptForUrl(url));
		ObjectNode tool = body.putArray("tools").addObject();
		tool.put("type", "web_search");
		tool.put("external_web_access", true);
		tool.putObject("filters").putArray("allowed_domains").add(host(url));
		return write(body);
	}

	private String buildPageBody(String url, String pageContent) {
		return write(baseBody(CarAdSchema.prompt(url, pageContent)));
	}

	private ObjectNode baseBody(String prompt) {
		LlmProperties.Provider provider = properties.getOpenai();
		ObjectNode body = objectMapper.createObjectNode();
		body.put("model", blankToDefault(provider.getModel(), "gpt-6.2-astra"));
		body.put("store", false);
		body.put("input", prompt);
		ObjectNode format = body.putObject("text").putObject("format");
		format.put("type", "json_schema");
		format.put("name", "car_ad");
		format.put("strict", true);
		format.set("schema", schema.copy());
		return body;
	}

	private String write(ObjectNode body) {
		try {
			return objectMapper.writeValueAsString(body);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Requête OpenAI invalide");
		}
	}

	private String host(String url) {
		String host = URI.create(url).getHost();
		if (host == null || host.isBlank()) {
			throw new IllegalStateException("URL sans domaine");
		}
		return host.toLowerCase();
	}

	private JsonNode readStructuredJson(String responseBody) {
		JsonNode root;
		try {
			root = objectMapper.readTree(responseBody);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Réponse OpenAI non JSON: " + LlmHttp.abbreviate(responseBody));
		}
		if (root.hasNonNull("error")) {
			throw new IllegalStateException(root.path("error").path("message").asText("erreur OpenAI"));
		}
		JsonNode output = root.path("output");
		if (output.isArray()) {
			for (JsonNode item : output) {
				if (!"message".equals(item.path("type").asText())) {
					continue;
				}
				for (JsonNode content : item.path("content")) {
					String type = content.path("type").asText();
					if ("refusal".equals(type)) {
						throw new IllegalStateException(content.path("refusal").asText("refus du modèle"));
					}
					if ("output_text".equals(type)) {
						return parseJson(content.path("text").asText());
					}
				}
			}
		}
		String outputText = root.path("output_text").asText("");
		if (!outputText.isBlank()) {
			return parseJson(outputText);
		}
		throw new IllegalStateException("Réponse structurée OpenAI absente");
	}

	private JsonNode parseJson(String text) {
		String trimmed = text == null ? "" : text.trim();
		if (trimmed.startsWith("```")) {
			int firstLine = trimmed.indexOf('\n');
			int lastFence = trimmed.lastIndexOf("```");
			if (firstLine >= 0 && lastFence > firstLine) {
				trimmed = trimmed.substring(firstLine + 1, lastFence).trim();
			}
		}
		try {
			return objectMapper.readTree(trimmed);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("JSON OpenAI illisible: " + LlmHttp.abbreviate(trimmed));
		}
	}

	private String blankToDefault(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}
}
