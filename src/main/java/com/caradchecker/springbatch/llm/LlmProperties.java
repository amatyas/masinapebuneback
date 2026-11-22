package com.caradchecker.springbatch.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

	private String provider = "gemini";
	private String adExtractorProvider = "openai";
	private String adExtractorModel = "gpt-6.2-astra";
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
