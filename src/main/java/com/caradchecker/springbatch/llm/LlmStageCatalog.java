package com.caradchecker.springbatch.llm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.caradchecker.springbatch.adrequest.AnalysisStage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class LlmStageCatalog {

	private final Map<AnalysisStage, JsonNode> formats = new EnumMap<>(AnalysisStage.class);
	private final Map<AnalysisStage, String> prompts = new EnumMap<>(AnalysisStage.class);

	public LlmStageCatalog(ObjectMapper objectMapper) throws IOException {
		load(objectMapper, AnalysisStage.LLM1_AD_EXTRACTOR, "llm/llm1-system.md", "llm/llm1-schema.json");
		load(objectMapper, AnalysisStage.LLM2_MECHANICAL_CHECK, "llm/llm2-system.md", "llm/llm2-schema.json");
		load(objectMapper, AnalysisStage.LLM3_MARKET_PRICE_CHECK, "llm/llm3-system.md", "llm/llm3-schema.json");
		load(objectMapper, AnalysisStage.LLM4_AD_CHECK, "llm/llm4-system.md", "llm/llm4-schema.json");
		load(objectMapper, AnalysisStage.LLM5_AD_VERDICT, "llm/llm5-system.md", "llm/llm5-schema.json");
	}

	public String systemPrompt(AnalysisStage stage) {
		return prompts.get(stage);
	}

	public String schemaName(AnalysisStage stage) {
		return formats.get(stage).path("name").asText(stage.name().toLowerCase());
	}

	public JsonNode openAiFormat(AnalysisStage stage) {
		return formats.get(stage).deepCopy();
	}

	public JsonNode geminiSchema(AnalysisStage stage) {
		return formats.get(stage).path("schema").deepCopy();
	}

	private void load(ObjectMapper objectMapper, AnalysisStage stage, String promptPath, String schemaPath) throws IOException {
		prompts.put(stage, read(promptPath));
		formats.put(stage, objectMapper.readTree(read(schemaPath)));
	}

	private String read(String path) throws IOException {
		try (InputStream input = new ClassPathResource(path).getInputStream()) {
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
