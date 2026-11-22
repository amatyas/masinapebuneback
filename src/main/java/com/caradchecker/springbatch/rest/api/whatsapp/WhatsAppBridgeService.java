package com.caradchecker.springbatch.rest.api.whatsapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.caradchecker.springbatch.rest.api.whatsapp.WhatsAppGraphClient.DownloadedMedia;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class WhatsAppBridgeService {

	private static final Logger log = LoggerFactory.getLogger(WhatsAppBridgeService.class);
	private static final Pattern LINK_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);

	private final WhatsAppProperties properties;
	private final WhatsAppGraphClient graphClient;
	private final String multipartUrl;

	public WhatsAppBridgeService(WhatsAppProperties properties, WhatsAppGraphClient graphClient,
			@Value("${webservice.multipart-url:}") String multipartUrl) {
		this.properties = properties;
		this.graphClient = graphClient;
		this.multipartUrl = multipartUrl;
	}

	@Async("whatsappTaskExecutor")
	public void processIncomingMessage(String senderName, JsonNode message) {
		try {
			String senderPhone = message.path("from").asText("");
			String messageId = message.path("id").asText("");
			String type = message.path("type").asText("unknown");
			if (senderPhone.isBlank()) {
				return;
			}

			graphClient.sendText(senderPhone, properties.getAckMessage());

			ExtractedContent content = extract(message, type);
			DownloadedMedia media = null;
			if (StringUtils.isNotBlank(content.mediaId())) {
				Optional<DownloadedMedia> downloaded = graphClient.downloadMedia(content.mediaId());
				media = downloaded.orElse(null);
			}

			log.info("WhatsApp message id={} from={} name={} type={} text={} links={} file={} bytes={}",
					messageId, senderPhone, senderName, type, content.text(), content.links(),
					content.fileName(), media == null ? 0 : media.bytes().length);

			graphClient.forwardMultipart(multipartUrl, senderPhone, type, content.text(),
					String.join(" ", content.links()), media, content.fileName());
		} catch (Exception e) {
			log.error("WhatsApp message processing failed", e);
		}
	}

	private ExtractedContent extract(JsonNode message, String type) {
		JsonNode node = message.path(type);
		String text = "";
		String mediaId = "";
		String fileName = "";
		switch (type) {
			case "text" -> text = node.path("body").asText("");
			case "image", "video", "audio", "sticker", "document" -> {
				mediaId = node.path("id").asText("");
				text = node.path("caption").asText("");
				fileName = node.path("filename").asText("");
			}
			case "location" -> text = node.path("latitude").asText("") + "," + node.path("longitude").asText("")
					+ " " + node.path("name").asText("") + " " + node.path("address").asText("");
			case "button" -> text = node.path("text").asText("");
			case "interactive" -> text = firstNonBlank(
					node.path("button_reply").path("title").asText(""),
					node.path("list_reply").path("title").asText(""));
			default -> text = node.path("body").asText("");
		}
		return new ExtractedContent(text.trim(), extractLinks(text), mediaId, fileName);
	}

	private List<String> extractLinks(String text) {
		List<String> links = new ArrayList<>();
		if (text == null || text.isBlank()) {
			return links;
		}
		Matcher matcher = LINK_PATTERN.matcher(text);
		while (matcher.find()) {
			links.add(matcher.group());
		}
		return links;
	}

	private String firstNonBlank(String first, String second) {
		return StringUtils.isNotBlank(first) ? first : second;
	}

	private record ExtractedContent(String text, List<String> links, String mediaId, String fileName) {
	}
}
