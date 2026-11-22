package com.caradchecker.springbatch.config;

import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutorConfig {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	
	
	@Bean(name = "heartbeatTaskExecutor")
    public Executor heartbeatTaskExecutor() {
        int nbThreads = 25;
        try {
            Map<String, String> properties = BatchProperties.getProperties(jdbcTemplate);
            if (properties.get(Properties.HEARTBEAT_THREADS.key()) != null) {
                nbThreads = Integer.parseInt(properties.get(Properties.HEARTBEAT_THREADS.key()));
            }
        } catch (Exception e) {
            // Log de repli
        }

        // Utilisation du composant natif Spring, parfaitement managé
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(nbThreads);
        executor.setMaxPoolSize(nbThreads);
        executor.setQueueCapacity(10000); // Important pour ne pas saturer la mémoire si Cisco bombarde
        executor.setThreadNamePrefix("heartbeat_thread-");
        executor.initialize(); // Initialisation obligatoire du pool Spring
        return executor;
    }
	


}
