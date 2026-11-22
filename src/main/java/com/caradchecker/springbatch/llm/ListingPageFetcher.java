package com.caradchecker.springbatch.llm;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class ListingPageFetcher {

	private static final int MAX_BYTES = 2_000_000;
	private static final int MAX_CONTENT_CHARS = 80_000;
	private static final int MAX_REDIRECTS = 5;
	private static final Pattern NEXT_DATA = Pattern.compile("<script id=\"__NEXT_DATA__\"[^>]*>(.*?)</script>", Pattern.DOTALL);
	private static final Set<String> DROPPED_FIELDS = Set.of(
			"images", "valueAddedServices", "badges", "aiInsight", "packages", "advertPriceForPriceDrop", "h2v", "adFromAdCore");

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	public ListingPageFetcher(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(15))
				.followRedirects(HttpClient.Redirect.NEVER)
				.build();
	}

	public String fetch(String url) {
		String html = download(URI.create(url));
		String embedded = embeddedAdvert(html);
		if (embedded != null) {
			log.info("preloading page source=next-data chars={}", embedded.length());
			return embedded;
		}
		String text = visibleText(html);
		if (text.isBlank()) {
			throw new IllegalStateException("Page de l'annonce vide");
		}
		log.info("preloading page source=html chars={}", text.length());
		return limit(text);
	}

	private String download(URI start) {
		URI uri = start;
		for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
			assertPublicHttp(uri);
			HttpRequest request = HttpRequest.newBuilder(uri)
					.timeout(Duration.ofSeconds(30))
					.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
					.header("Accept", "text/html,application/xhtml+xml")
					.header("Accept-Language", "ro-RO,ro;q=0.9,fr;q=0.8,en;q=0.7")
					.GET()
					.build();
			HttpResponse<byte[]> response;
			try {
				response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Lecture de l'annonce interrompue");
			} catch (IOException ex) {
				throw new IllegalStateException("Page de l'annonce injoignable: " + ex.getMessage());
			}
			int status = response.statusCode();
			if (status >= 300 && status < 400) {
				String location = response.headers().firstValue("location").orElse("");
				if (location.isBlank()) {
					throw new IllegalStateException("Redirection de l'annonce sans cible");
				}
				uri = uri.resolve(location);
				continue;
			}
			if (status / 100 != 2) {
				throw new IllegalStateException("Page de l'annonce HTTP " + status);
			}
			byte[] body = response.body();
			if (body == null || body.length == 0) {
				throw new IllegalStateException("Page de l'annonce vide");
			}
			if (body.length > MAX_BYTES) {
				throw new IllegalStateException("Page de l'annonce trop volumineuse");
			}
			return new String(body, StandardCharsets.UTF_8);
		}
		throw new IllegalStateException("Trop de redirections pour l'annonce");
	}

	private String embeddedAdvert(String html) {
		Matcher matcher = NEXT_DATA.matcher(html);
		if (!matcher.find()) {
			return null;
		}
		try {
			JsonNode advert = objectMapper.readTree(matcher.group(1)).path("props").path("pageProps").path("advert");
			if (!advert.isObject()) {
				return null;
			}
			ObjectNode copy = (ObjectNode) advert.deepCopy();
			List<String> removed = new ArrayList<>();
			copy.fieldNames().forEachRemaining(name -> {
				if (DROPPED_FIELDS.contains(name) || name.startsWith("show")) {
					removed.add(name);
				}
			});
			copy.remove(removed);
			return limit(objectMapper.writeValueAsString(copy));
		} catch (IOException ex) {
			return null;
		}
	}

	private String visibleText(String html) {
		String withoutScripts = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
				.replaceAll("(?is)<style[^>]*>.*?</style>", " ")
				.replaceAll("(?is)<noscript[^>]*>.*?</noscript>", " ");
		return withoutScripts.replaceAll("(?is)<[^>]+>", " ")
				.replace("&nbsp;", " ")
				.replace("&amp;", "&")
				.replace("&quot;", "\"")
				.replace("&#39;", "'")
				.replaceAll("\\s+", " ")
				.trim();
	}

	private void assertPublicHttp(URI uri) {
		String scheme = uri.getScheme();
		if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) || uri.getHost() == null) {
			throw new IllegalStateException("URL de l'annonce non publique");
		}
		String host = uri.getHost().toLowerCase();
		if (host.equals("localhost") || host.endsWith(".local") || host.endsWith(".internal")) {
			throw new IllegalStateException("URL de l'annonce non publique");
		}
		try {
			for (InetAddress address : InetAddress.getAllByName(host)) {
				if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()) {
					throw new IllegalStateException("URL de l'annonce non publique");
				}
			}
		} catch (IOException ex) {
			throw new IllegalStateException("Domaine de l'annonce introuvable");
		}
	}

	private String limit(String value) {
		return value.length() <= MAX_CONTENT_CHARS ? value : value.substring(0, MAX_CONTENT_CHARS);
	}
}
