package com.caradchecker.springbatch.mailer;

import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.mailer.MailerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import com.caradchecker.springbatch.config.SMTPConfiguration;

@Configuration
public class MailServiceFactory {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public Mailer createMailer(SMTPConfiguration smtpConfig) {
         String host = smtpConfig.getHost();
  	      boolean isAuthDisabled = smtpConfig.isDisableAuth();
  	      TransportStrategy protocol = TransportStrategy.SMTP;
  	      if(host.contains("ssl://")) {
  	    	  host = host.replaceAll("ssl://", "");
  	    	  protocol = TransportStrategy.SMTPS;
  	      }
  	      else if(host.contains("tls://")) {
  	    	  host = host.replaceAll("tls://", "");
  	    	  protocol = TransportStrategy.SMTP_TLS;
  	      }	
  	      if(isAuthDisabled) {
  	    	  log.info("SMTP : Authetication is disable. Transport: "+protocol.toString());
  	      } else {
  	    	log.info("SMTP : Transport: "+protocol.toString());
  	      }
  	      Mailer mailer = MailerBuilder
	              .withSMTPServer(host, smtpConfig.getPort().intValue(), isAuthDisabled ? null : smtpConfig.getFromEmail(), isAuthDisabled ? null : smtpConfig.getPassword())
	              .withTransportStrategy(protocol)
	              .withSessionTimeout(10 * 1000)
	              //.clearEmailAddressCriteria() // turns off email validation
	              //.withProperty("mail.smtp.sendpartial", true)
	              .withDebugLogging(false)
	              .buildMailer();
  	      return mailer; 
        }
}
