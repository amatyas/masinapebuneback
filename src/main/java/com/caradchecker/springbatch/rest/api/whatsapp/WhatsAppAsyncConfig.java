package com.caradchecker.springbatch.rest.api.whatsapp;

import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(WhatsAppProperties.class)
public class WhatsAppAsyncConfig {

	@Bean(name = "whatsappTaskExecutor")
	public Executor whatsappTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(16);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("whatsapp-");
		executor.initialize();
		return executor;
	}
}
