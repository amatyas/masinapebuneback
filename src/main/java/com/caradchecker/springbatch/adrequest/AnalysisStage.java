package com.caradchecker.springbatch.adrequest;

public enum AnalysisStage {
	LLM1_AD_EXTRACTOR,
	LLM2_MECHANICAL_CHECK,
	LLM3_MARKET_PRICE_CHECK,
	LLM4_AD_CHECK,
	LLM5_AD_VERDICT,
	REPORT
}
