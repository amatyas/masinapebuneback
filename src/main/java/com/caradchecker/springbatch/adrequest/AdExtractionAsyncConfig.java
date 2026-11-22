package com.caradchecker.springbatch.adrequest;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.caradchecker.springbatch.llm.LlmProperties;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class AdExtractionAsyncConfig {

	@Bean(name = "adExtractionExecutor")
	public Executor adExtractionExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("ad-extraction-");
		executor.initialize();
		return executor;
	}

	@Bean
	public HttpClient llmHttpClient() {
		return HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(20))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}
}
