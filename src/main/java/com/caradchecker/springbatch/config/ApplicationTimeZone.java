package com.caradchecker.springbatch.config;

import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JVM timezone of the Java application ({@link ZoneId#systemDefault()}).
 */
@Component
public class ApplicationTimeZone {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ZoneId zoneId;

	public ApplicationTimeZone() {
		this.zoneId = ZoneId.systemDefault();
		log.info("Application timezone: {}", zoneId);
	}

	public ZoneId getZoneId() {
		return zoneId;
	}
}
