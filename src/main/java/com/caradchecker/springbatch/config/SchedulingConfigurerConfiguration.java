package com.caradchecker.springbatch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
public class SchedulingConfigurerConfiguration implements SchedulingConfigurer {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    	log.info("Setting ThreadPoolTaskScheduler to 20 threads");
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(20);
        taskScheduler.initialize();
        taskRegistrar.setTaskScheduler(taskScheduler);
        
    }
}