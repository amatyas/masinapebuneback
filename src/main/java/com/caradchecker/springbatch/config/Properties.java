package com.caradchecker.springbatch.config;

public enum Properties {
	
	SERVER_URL							("server.url"),
	CLIENT_PROFILE 						("client_profile"),
	IS_VISIOLOGIC_OBTP_ENABLE 			("is_visiologic_OBTP_enable"),
	IS_DEMO								("is_demo"),
	
	SMTP_HOST							("smtp_host"),
	SMTP_PORT 							("smtp_port"),
	SMTP_ALIAS 							("smtp_alias"),
	SMTP_USERNAME 						("smtp_username"),
	SMTP_PASS 							("smtp_password"),
	SMTP_DISABLE_AUTH					("smtp_disable_auth"),
	
	PROXY_HOST							("proxy_host"),
	PROXY_PORT							("proxy_port"),
	PROXY_TYPE 							("proxy_type"),
	PROXY_USER 							("proxy_user"),
	PROXY_PASS 							("proxy_password"),
	
	CHECKSTATUS_THREADS					("batch.checkstatus.threads"),
	CHECKSTATUS_DELAY					("batch.checkstatus.delay.minutes"),
	
	CHECK_SCREEN_STATUS_DELAY			("batch.checkstatus_screen.delay.minutes"),
	CHECK_SCREEN_STATUS_THREADS			("batch.checkstatus_screen.threads"),

	CHECKSTATUS_BARCO_THREADS			("batch.checkstatus_barco.threads"),
	CHECKSTATUS_BARCO_DELAY				("batch.checkstatus_barco.delay.minutes"),
	
	CHECKSTATUS_AIRMEDIA_THREADS		("batch.checkstatus_airmedia.threads"),
	CHECKSTATUS_AIRMEDIA_DELAY			("batch.checkstatus_airmedia.delay.minutes"),

	CHECKSTATUS_ROOMPANEL_THREADS		("batch.checkstatus_roompanel.threads"),
	CHECKSTATUS_ROOMPANEL_DELAY			("batch.checkstatus_roompanel.delay.minutes"),
	
	CHECKSTATUS_OTHERDEVICE_THREADS		("batch.checkstatus_otherdevice.threads"),
	CHECKSTATUS_OTHERDEVICE_DELAY		("batch.checkstatus_otherdevice.delay.minutes"),
	
	CRONJOBS_RUNNER_THREADS				("batch.cronjobrunner.threads"),
	
	PURGE_CALL_HISTORY_DELAY_MONTHS 	("purge.callhistory.delay.months"),
	PURGE_HEARTBEAT_HISTORY_DELAY_MONTHS ("purge.heartbeathistory.delay.months"),
	PURGE_CODECDOWNTIME_HISTORY_DELAY_MONTHS ("purge.codecdownhistory.delay.months"),
	PURGE_CRONJOBS_HISTORY_DELAY_MONTHS ("purge.cronjobs.delay.months"),
	PURGE_MACROS_TRACKING_DELAY_MONTHS 	("purge.macrostracking.delay.months"),
	PURGE_RATINGS_DELAY_MONTHS 			("purge.ratings.delay.months"),
	PURGE_USER_REQUESTS_DELAY_MONTHS 	("purge.userstickets.delay.months"),
	PURGE_PRESENTATION_HISTORY_DELAY_MONTHS("purge.presentationhistory.delay.months"),
	PURGE_WEBEX_HISTORY_DELAY_MONTHS	("purge.webex.history.delay.months"),
	PURGE_MSTEAMS_HISTORY_DELAY_MONTHS	("purge.msteams.history.delay.months"),
	PURGE_JABBER_HISTORY_DELAY_MONTHS	("purge.jabber.history.delay.months"),
	
	WEBEX_API_SYNC_MEETINGS_THREADS			("batch.webexapi.syncmeetings.threads"),
	WEBEX_STATUS_REMOTE_URL 				("batch.webex.status.remote.url"),

	LDAP_RESOURCE_TRANSFORMER 					("batch.activedirectory.resources.transformer"),
	LDAP_RESOURCE_TRANSFORMER_CAPGEMINI_VALUE 	("capgemini"),
	
	REPORTING_THREADS 					("batch.reporting.send.threads"),
	REPORTING_CHROME_DRIVER_PATH_EXE	("batch.reporting.chromedriver.path"),
	REPORTING_CHROME_PATH_EXE			("batch.reporting.chromeexe.path"),
	REPORTING_DOWNLOAD_FOLDER			("batch.reporting.download.folder"),
	REPORTING_SUBSCRIPTION_SHAREDFOLDER	("batch.reporting.subscription.sharedfolder"),
	
	JABBER_IMPORT_FOLDER 				("batch.jabber.import.folder"),
	JABBER_IMPORT_LOCAL_FOLDER 			("batch.jabber.import.local.folder"),
	JABBER_IMPORT_PARSING_THREADS 		("batch.jabber.import.parsing.threads"),
	JABBER_IMPORT_FILE_LOT_SIZE 		("batch.jabber.import.filelot.size"),
	JABBER_DEVICE_NAME_STRING			("batch.jabber.import.device.name.contains"),
	JABBER_DOMAIN_FINDER				("batch.jabber.domain.finder"),
	JABBER_DOMAIN_FINDER_CREDIT_AGRICOLE_VALUE 	("credit-agricole"),
	
	HEARTBEAT_THREADS 					("batch.http.api.heartbeat.threads"),
	
	RCP_IMPORT_FOLDER 					("batch.rcp.import.folder"),

	ARCHIVER_CONFIG_DIRECTORY			("batch.device.config.archive.directory"),
	
	
	CHECKSTATUS_PRINTER_THREADS			("batch.checkstatus_printer.threads"),
	CHECKSTATUS_PRINTER_DELAY			("batch.checkstatus_printer.delay.minutes"),

	OLLAMA_BASE_URL						("ollama.base-url"),
	OLLAMA_MODEL						("ollama.model"),
	OLLAMA_PRELOAD						("ollama.preload"),
	OLLAMA_KEEP_ALIVE					("ollama.keep-alive"),
	OLLAMA_API_KEY						("ollama.api-key"),
	AI_ENGINE_TYPE						("ai.engine-type")

	;
	
	
	



	
	
	private final String name; 
	
	Properties(String name) {
        this.name = name;
    }
	
	public String key() { return name; }

}
