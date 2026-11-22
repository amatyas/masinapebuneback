package com.caradchecker.springbatch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@DependsOn("databaseConfig")
@EnableBatchProcessing
@EnableScheduling
public class BatchConfiguration {
	
    @Autowired
    private JobLauncher jobLauncher;
	
	
	@Autowired
	@Qualifier(value = "jobPurgeData")
	private Job jobPurgeData;
	
	
 	// run at 3 AM every morning
 	@Scheduled(cron = "0 0 3 * * ?")
 	public void performPurgeDataJob() throws Exception {
 		JobParameters params = new JobParametersBuilder().addString("JobID", "PurgeData" +String.valueOf(System.currentTimeMillis())).toJobParameters();
 	 	jobLauncher.run(jobPurgeData, params);
 	}
 	
}
