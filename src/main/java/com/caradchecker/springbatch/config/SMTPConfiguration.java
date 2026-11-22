package com.caradchecker.springbatch.config;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SMTPConfiguration {
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private String host;
	private Integer port;
	private String fromName;
	private String fromEmail;
	private String password;
	private boolean disableAuth;
	
	public SMTPConfiguration(Map<String, String> properties) {
		host = properties.get(Properties.SMTP_HOST.key());
		port = !StringUtils.isBlank(properties.get(Properties.SMTP_PORT.key()))? Integer.parseInt(properties.get(Properties.SMTP_PORT.key())):null;
		fromName = properties.get(Properties.SMTP_ALIAS.key());
		fromEmail = properties.get(Properties.SMTP_USERNAME.key());
		password = properties.get(Properties.SMTP_PASS.key());
		disableAuth = !StringUtils.isBlank(properties.get(Properties.SMTP_DISABLE_AUTH.key())) && "1".equals(properties.get(Properties.SMTP_DISABLE_AUTH.key()))? true:false;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getFromName() {
		return fromName;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	public String getFromEmail() {
		return fromEmail;
	}

	public void setFromEmail(String fromEmail) {
		this.fromEmail = fromEmail;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Logger getLog() {
		return log;
	}

	public boolean isDisableAuth() {
		return disableAuth;
	}

	public void setDisableAuth(boolean disableAuth) {
		this.disableAuth = disableAuth;
	}
	
	
	
	
	
}
