package com.caradchecker.springbatch.rest.api.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {

	private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

	private final WhatsAppProperties properties;
	private final WhatsAppBridgeService bridgeService;

	public WhatsAppWebhookController(WhatsAppProperties properties, WhatsAppBridgeService bridgeService) {
		this.properties = properties;
		this.bridgeService = bridgeService;
	}

	@GetMapping({ "", "/verifyWebhook" }) 
	public ResponseEntity<String> verifyWebhook(
			@RequestParam(name = "hub.mode", required = false) String mode,
			@RequestParam(name = "hub.verify_token", required = false) String token,
			@RequestParam(name = "hub.challenge", required = false) String challenge) {
		if (mode == null && token == null && challenge == null) {
			return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("webhook ok");
		}
		if ("subscribe".equals(mode) && properties.getVerifyToken() != null && properties.getVerifyToken().equals(token)) {
			return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(challenge);
		}
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token invalide");
	}

	@PostMapping
	public ResponseEntity<Void> receiveMessage(HttpServletRequest request, @RequestBody JsonNode payload) {
		log.info("WhatsApp webhook POST {} contentType={} remote={}", request.getRequestURI(), request.getContentType(), request.getRemoteAddr());
		log.info("WhatsApp raw payload:\n{}", payload.toPrettyString());

		JsonNode entries = payload.path("entry");
		if (!entries.isArray() || entries.isEmpty()) {
			log.info("WhatsApp payload has no entry. object={}", payload.path("object").asText(""));
			return ResponseEntity.ok().build();
		}

		int entryIndex = 0;
		for (JsonNode entry : entries) {
			log.info("WhatsApp entry[{}] id={}", entryIndex, entry.path("id").asText(""));
			int changeIndex = 0;
			for (JsonNode change : entry.path("changes")) {
				JsonNode value = change.path("value");
				JsonNode metadata = value.path("metadata");
				JsonNode contacts = value.path("contacts");
				JsonNode messages = value.path("messages");
				JsonNode statuses = value.path("statuses");
				log.info("WhatsApp change[{}] field={} phoneNumberId={} displayPhone={} contacts={} messages={} statuses={}",
						changeIndex,
						change.path("field").asText(""),
						metadata.path("phone_number_id").asText(""),
						metadata.path("display_phone_number").asText(""),
						contacts.size(),
						messages.size(),
						statuses.size());
				if (contacts.isArray()) {
					int contactIndex = 0;
					for (JsonNode contact : contacts) {
						log.info("WhatsApp contact[{}] waId={} name={}", contactIndex, contact.path("wa_id").asText(""),
								contact.path("profile").path("name").asText(""));
						contactIndex++;
					}
				}
				if (statuses.isArray() && !statuses.isEmpty()) {
					int statusIndex = 0;
					for (JsonNode status : statuses) {
						log.info("WhatsApp status[{}] id={} status={} recipient={} timestamp={} conversation={} pricing={} errors={}",
								statusIndex,
								status.path("id").asText(""),
								status.path("status").asText(""),
								status.path("recipient_id").asText(""),
								status.path("timestamp").asText(""),
								status.path("conversation").toString(),
								status.path("pricing").toString(),
								status.path("errors").toString());
						statusIndex++;
					}
				}
				if (!messages.isArray() || messages.isEmpty()) {
					log.info("WhatsApp change[{}] has no incoming message", changeIndex);
					changeIndex++;
					continue;
				}
				String senderName = contacts.path(0).path("profile").path("name").asText("");
				int messageIndex = 0;
				for (JsonNode message : messages) {
					String type = message.path("type").asText("");
					log.info("WhatsApp message[{}] id={} from={} timestamp={} type={} name={}",
							messageIndex,
							message.path("id").asText(""),
							message.path("from").asText(""),
							message.path("timestamp").asText(""),
							type,
							senderName);
					log.info("WhatsApp message[{}] content:\n{}", messageIndex, message.toPrettyString());
					if (!type.isBlank()) {
						log.info("WhatsApp message[{}] {} node:\n{}", messageIndex, type, message.path(type).toPrettyString());
					}
					bridgeService.processIncomingMessage(senderName, message);
					messageIndex++;
				}
				changeIndex++;
			}
			entryIndex++;
		}
		return ResponseEntity.ok().build();
	}
}
