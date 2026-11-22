package com.caradchecker.springbatch.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

	private String provider = "gemini";
	private String adExtractorProvider = "openai";
	private String adExtractorModel = "gpt-6.2-astra";
	private String mechanicalModel = "gpt-6.2-sol";
	private String marketModel = "gpt-6.1-terra";
	private String auditModel = "gpt-6.1-terra";
	private String verdictModel = "gpt-6.2-sol";
	private Provider openai = new Provider();
	private Provider gemini = new Provider();

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getAdExtractorProvider() {
		return adExtractorProvider;
	}

	public void setAdExtractorProvider(String adExtractorProvider) {
		this.adExtractorProvider = adExtractorProvider;
	}

	public String getAdExtractorModel() {
		return adExtractorModel;
	}

	public void setAdExtractorModel(String adExtractorModel) {
		this.adExtractorModel = adExtractorModel;
	}

	public String getMechanicalModel() {
		return mechanicalModel;
	}

	public void setMechanicalModel(String mechanicalModel) {
		this.mechanicalModel = mechanicalModel;
	}

	public String getMarketModel() {
		return marketModel;
	}

	public void setMarketModel(String marketModel) {
		this.marketModel = marketModel;
	}

	public String getAuditModel() {
		return auditModel;
	}

	public void setAuditModel(String auditModel) {
		this.auditModel = auditModel;
	}

	public String getVerdictModel() {
		return verdictModel;
	}

	public void setVerdictModel(String verdictModel) {
		this.verdictModel = verdictModel;
	}

	public String modelFor(String stage) {
		String configured = switch (stage) {
			case "LLM2_MECHANICAL_CHECK" -> mechanicalModel;
			case "LLM3_MARKET_PRICE_CHECK" -> marketModel;
			case "LLM4_AD_CHECK" -> auditModel;
			case "LLM5_AD_VERDICT" -> verdictModel;
			default -> adExtractorModel;
		};
		if (configured != null && !configured.isBlank()) {
			return configured.trim();
		}
		return openai.getModel();
	}

	public Provider getOpenai() {
		return openai;
	}

	public void setOpenai(Provider openai) {
		this.openai = openai;
	}

	public Provider getGemini() {
		return gemini;
	}

	public void setGemini(Provider gemini) {
		this.gemini = gemini;
	}

	public static class Provider {

		private String apiKey = "";
		private String model = "";
		private String url = "";

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}

		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public boolean hasApiKey() {
			return apiKey != null && !apiKey.isBlank();
		}
	}
}
