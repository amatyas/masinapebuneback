package com.caradchecker.springbatch;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CarAdCheckerBatchApplication {
	public static void main(String[] args) throws Exception {
		System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
		SpringApplication app = new SpringApplication(CarAdCheckerBatchApplication.class);
        // app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
 	}
}
