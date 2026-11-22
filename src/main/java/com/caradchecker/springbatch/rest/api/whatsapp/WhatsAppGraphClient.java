package com.caradchecker.springbatch.rest.api.whatsapp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class WhatsAppGraphClient {

	private static final Logger log = LoggerFactory.getLogger(WhatsAppGraphClient.class);

	private final WhatsAppProperties properties;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	public WhatsAppGraphClient(WhatsAppProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	public void sendText(String to, String body) {
		if (!credentialsPresent()) {
			log.warn("WhatsApp token or phone-number-id is missing; ack not sent to {}", to);
			return;
		}
		try {
			ObjectNode text = objectMapper.createObjectNode();
			text.put("body", body);
			ObjectNode payload = objectMapper.createObjectNode();
			payload.put("messaging_product", "whatsapp");
			payload.put("to", to);
			payload.put("type", "text");
			payload.set("text", text);

			HttpRequest request = authorized(properties.getApiUrl() + "/" + properties.getPhoneNumberId() + "/messages")
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() >= 300) {
				log.error("WhatsApp send failed status={} body={}", response.statusCode(), response.body());
			}
		} catch (Exception e) {
			log.error("WhatsApp send failed for {}", to, e);
		}
	}

	public Optional<DownloadedMedia> downloadMedia(String mediaId) {
		if (!credentialsPresent() || mediaId == null || mediaId.isBlank()) {
			return Optional.empty();
		}
		try {
			HttpRequest metaRequest = authorized(properties.getApiUrl() + "/" + mediaId).GET().build();
			HttpResponse<String> metaResponse = httpClient.send(metaRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (metaResponse.statusCode() >= 300) {
				log.error("WhatsApp media lookup failed status={} body={}", metaResponse.statusCode(), metaResponse.body());
				return Optional.empty();
			}
			JsonNode meta = objectMapper.readTree(metaResponse.body());
			String url = meta.path("url").asText("");
			String mimeType = meta.path("mime_type").asText("application/octet-stream");
			if (url.isBlank()) {
				return Optional.empty();
			}
			HttpRequest fileRequest = authorized(url).GET().build();
			HttpResponse<byte[]> fileResponse = httpClient.send(fileRequest, HttpResponse.BodyHandlers.ofByteArray());
			if (fileResponse.statusCode() >= 300) {
				log.error("WhatsApp media download failed status={}", fileResponse.statusCode());
				return Optional.empty();
			}
			return Optional.of(new DownloadedMedia(fileResponse.body(), mimeType));
		} catch (Exception e) {
			log.error("WhatsApp media download failed for {}", mediaId, e);
			return Optional.empty();
		}
	}

	public void forwardMultipart(String url, String sender, String type, String text, String links, DownloadedMedia media, String fileName) {
		if (url == null || url.isBlank()) {
			return;
		}
		try {
			String boundary = "----WhatsAppBoundary" + System.currentTimeMillis();
			byte[] body = multipartBody(boundary, sender, type, text, links, media, fileName);
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.timeout(Duration.ofSeconds(30))
					.header("Content-Type", "multipart/form-data; boundary=" + boundary)
					.POST(HttpRequest.BodyPublishers.ofByteArray(body))
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() >= 300) {
				log.error("Multipart forward failed status={} body={}", response.statusCode(), response.body());
			}
		} catch (Exception e) {
			log.error("Multipart forward failed", e);
		}
	}

	private boolean credentialsPresent() {
		return properties.getAccessToken() != null && !properties.getAccessToken().isBlank()
				&& properties.getPhoneNumberId() != null && !properties.getPhoneNumberId().isBlank();
	}

	private HttpRequest.Builder authorized(String url) {
		return HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(30))
				.header("Authorization", "Bearer " + properties.getAccessToken());
	}

	private byte[] multipartBody(String boundary, String sender, String type, String text, String links, DownloadedMedia media, String fileName) {
		StringBuilder head = new StringBuilder();
		appendField(head, boundary, "sender", sender);
		appendField(head, boundary, "type", type);
		appendField(head, boundary, "message", text == null ? "" : text);
		appendField(head, boundary, "links", links == null ? "" : links);
		byte[] headBytes = head.toString().getBytes(StandardCharsets.UTF_8);
		if (media == null || media.bytes() == null || media.bytes().length == 0) {
			return (head + "--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
		}
		String safeName = (fileName == null || fileName.isBlank()) ? "media" : fileName;
		String fileHead = "--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"file\"; filename=\"" + safeName + "\"\r\n"
				+ "Content-Type: " + media.mimeType() + "\r\n\r\n";
		byte[] fileHeadBytes = fileHead.getBytes(StandardCharsets.UTF_8);
		byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
		byte[] body = new byte[headBytes.length + fileHeadBytes.length + media.bytes().length + tail.length];
		System.arraycopy(headBytes, 0, body, 0, headBytes.length);
		System.arraycopy(fileHeadBytes, 0, body, headBytes.length, fileHeadBytes.length);
		System.arraycopy(media.bytes(), 0, body, headBytes.length + fileHeadBytes.length, media.bytes().length);
		System.arraycopy(tail, 0, body, headBytes.length + fileHeadBytes.length + media.bytes().length, tail.length);
		return body;
	}

	private void appendField(StringBuilder body, String boundary, String name, String value) {
		body.append("--").append(boundary).append("\r\n");
		body.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n");
		body.append(value == null ? "" : value).append("\r\n");
	}

	public record DownloadedMedia(byte[] bytes, String mimeType) {
	}
}
