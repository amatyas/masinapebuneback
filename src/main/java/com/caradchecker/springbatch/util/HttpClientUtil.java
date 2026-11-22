package com.caradchecker.springbatch.util;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.caradchecker.springbatch.config.Properties;

public class HttpClientUtil {
	
	private static final Logger log = LoggerFactory.getLogger(HttpClientUtil.class);
	/**
	  	the Connection Timeout (http.connection.timeout) – the time to establish the connection with the remote host
		the Socket Timeout (http.socket.timeout) – the time waiting for data – after establishing the connection; maximum time of inactivity between two data packets
		the Connection Manager Timeout (http.connection-manager.timeout) – the time to wait for a connection from the connection manager/pool
	 */
	private static int connectTimeoutSec = 30;
	private static int connectionRequestTimeoutSec = 30;
	private static int socketTimeoutSec = 5*60;
	
	public static HttpClient createHttpClient() {
		return createReusableHttpClient(socketTimeoutSec);
	}
	
	public static HttpClient createHttpClient(Integer socketTimeoutInSec) {
		return createReusableHttpClient(socketTimeoutInSec != null ? socketTimeoutInSec : socketTimeoutSec);
	}

	/**
	 * Shared/long-lived client: pooling + eviction of idle/expired connections to avoid leaks when reused.
	 */
	private static CloseableHttpClient createReusableHttpClient(int socketTimeoutInSec) {
		javax.net.ssl.SSLContext sslContext = null;
		try {
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
		} catch (Exception e) {}
		RequestConfig config = RequestConfig.custom()
				  .setConnectTimeout(connectTimeoutSec * 1000)
				  .setConnectionRequestTimeout(connectionRequestTimeoutSec * 1000)
				  .setSocketTimeout(socketTimeoutInSec * 1000).build();
		return HttpClientBuilder.create()
				.setDefaultRequestConfig(config)
				.setSSLContext(sslContext)
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.evictExpiredConnections()
				.evictIdleConnections(30, TimeUnit.SECONDS)
				.setMaxConnTotal(50)
				.setMaxConnPerRoute(20)
				.build();
	}
	
	public static CloseableHttpClient createHttpClientWithProxy(Integer socketTimeoutInSec, Map<String, String> properties) {
		String proxyHost = !StringUtils.isBlank(properties.get(Properties.PROXY_HOST.key())) ? properties.get(Properties.PROXY_HOST.key()) : null;
		int proxyPort = !StringUtils.isBlank(properties.get(Properties.PROXY_PORT.key())) ? Integer.parseInt(properties.get(Properties.PROXY_PORT.key())) : 0;
		String proxyUsername = !StringUtils.isBlank(properties.get(Properties.PROXY_USER.key())) ? properties.get(Properties.PROXY_USER.key()) : null;
		String proxyType = !StringUtils.isBlank(properties.get(Properties.PROXY_TYPE.key())) ? properties.get(Properties.PROXY_TYPE.key()) : null;
        String proxyPassword = !StringUtils.isBlank(properties.get(Properties.PROXY_PASS.key())) ? properties.get(Properties.PROXY_PASS.key()) : null;;
        
        if(proxyHost != null) {
        	log.info("Using "+proxyType+" proxy "+ proxyHost+":"+proxyPort + "("+proxyUsername+"/*****)");
        }
        return createHttpClient(socketTimeoutInSec, proxyHost,  proxyType, proxyPort, proxyUsername, proxyPassword);
	}
	
	
	private static CloseableHttpClient createHttpClient(Integer socketTimeoutInSec, String proxyHost,  String proxyType, int proxyPort, String proxyUsername, String proxyPassword) {
		javax.net.ssl.SSLContext sslContext = null;
		try {
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
		} catch (Exception e) {}
		
		RequestConfig config = RequestConfig.custom()
				  .setConnectTimeout(connectTimeoutSec * 1000)
				  .setConnectionRequestTimeout(connectionRequestTimeoutSec * 1000)
				  .setSocketTimeout(socketTimeoutInSec != null ? socketTimeoutInSec.intValue()*1000 : socketTimeoutSec * 1000).build();
		HttpClientBuilder builder = HttpClientBuilder.create()
				.setDefaultRequestConfig(config)
				.setSSLContext(sslContext)
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.evictExpiredConnections()
				.evictIdleConnections(30, TimeUnit.SECONDS)
				.setMaxConnTotal(50)
				.setMaxConnPerRoute(20); 
		if(!StringUtils.isBlank(proxyHost)) {
			HttpHost proxy = new HttpHost(proxyHost, proxyPort, proxyType);
	        builder.setProxy(proxy);
			if(!StringUtils.isBlank(proxyUsername)) {
				BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
		        credsProvider.setCredentials(
		            new AuthScope(proxyHost, proxyPort),
		            new UsernamePasswordCredentials(proxyUsername, proxyPassword)
		        );
				builder.setDefaultCredentialsProvider(credsProvider);
			}
		}
		return builder.build();
	}
	
	public static void closeHttpClient(HttpClient client) {
		HttpClientUtils.closeQuietly(client);
	}

	/**
	 * LAN reachability checks: short timeouts, no proxy, no redirect follow, trust-all TLS.
	 */
	public static CloseableHttpClient createPingHttpClient() {
		javax.net.ssl.SSLContext sslContext = null;
		try {
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
		} catch (Exception e) {
		}
		int pingTimeoutMs = 10 * 1000;
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(pingTimeoutMs)
				.setConnectionRequestTimeout(pingTimeoutMs)
				.setSocketTimeout(pingTimeoutMs)
				.setRedirectsEnabled(false)
				.build();
		return HttpClientBuilder.create()
				.setDefaultRequestConfig(config)
				.setSSLContext(sslContext)
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.evictExpiredConnections()
				.evictIdleConnections(30, TimeUnit.SECONDS)
				.setMaxConnTotal(100)
				.setMaxConnPerRoute(20)
				.build();
	}

	/**
	 * LAN AirMedia/Crestron: short timeouts, no proxy, follow redirects, trust-all TLS, per-request cookies.
	 */
	public static CloseableHttpClient createAirMediaHttpClient() {
		javax.net.ssl.SSLContext sslContext = null;
		try {
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
		} catch (Exception e) {
		}
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(10 * 1000)
				.setConnectionRequestTimeout(10 * 1000)
				.setSocketTimeout(30 * 1000)
				.setRedirectsEnabled(true)
				.setCookieSpec(org.apache.http.client.config.CookieSpecs.STANDARD)
				.build();
		return HttpClientBuilder.create()
				.setDefaultRequestConfig(config)
				.setSSLContext(sslContext)
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.evictExpiredConnections()
				.evictIdleConnections(30, TimeUnit.SECONDS)
				.setMaxConnTotal(100)
				.setMaxConnPerRoute(20)
				.build();
	}

	/**
	 * LAN Yealink RoomPanel: short timeouts, no proxy, follow redirects, trust-all TLS, per-request cookies.
	 */
	public static CloseableHttpClient createRoomPanelHttpClient() {
		javax.net.ssl.SSLContext sslContext = null;
		try {
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
		} catch (Exception e) {
		}
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(10 * 1000)
				.setConnectionRequestTimeout(10 * 1000)
				.setSocketTimeout(30 * 1000)
				.setRedirectsEnabled(true)
				.setCookieSpec(CookieSpecs.STANDARD)
				.build();
		return HttpClientBuilder.create()
				.setDefaultRequestConfig(config)
				.setSSLContext(sslContext)
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.evictExpiredConnections()
				.evictIdleConnections(30, TimeUnit.SECONDS)
				.setMaxConnTotal(100)
				.setMaxConnPerRoute(20)
				.build();
	}

	/**
	 * LAN Barco ClickShare: 10s timeout like PHP CURLOPT_TIMEOUT, no proxy, follow redirects, trust-all TLS.
	 */
	public static CloseableHttpClient createBarcoHttpClient() {
		javax.net.ssl.SSLContext sslContext = null;
		try {
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
		} catch (Exception e) {
		}
		int timeoutMs = 10 * 1000;
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeoutMs)
				.setConnectionRequestTimeout(timeoutMs)
				.setSocketTimeout(timeoutMs)
				.setRedirectsEnabled(true)
				.build();
		return HttpClientBuilder.create()
				.setDefaultRequestConfig(config)
				.setSSLContext(sslContext)
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.evictExpiredConnections()
				.evictIdleConnections(30, TimeUnit.SECONDS)
				.setMaxConnTotal(100)
				.setMaxConnPerRoute(20)
				.build();
	}

	/**
	 * LAN codec cronjobs: 30s connect like PHP CURLOPT_CONNECTTIMEOUT, no proxy, no redirect, trust-all TLS.
	 */
	public static CloseableHttpClient createCronjobHttpClient() {
		javax.net.ssl.SSLContext sslContext = null;
		try {
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
		} catch (Exception e) {
		}
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(30 * 1000)
				.setConnectionRequestTimeout(30 * 1000)
				.setSocketTimeout(5 * 60 * 1000)
				.setRedirectsEnabled(false)
				.build();
		return HttpClientBuilder.create()
				.setDefaultRequestConfig(config)
				.setSSLContext(sslContext)
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.evictExpiredConnections()
				.evictIdleConnections(30, TimeUnit.SECONDS)
				.setMaxConnTotal(100)
				.setMaxConnPerRoute(20)
				.build();
	}

	/**
	 * LAN codec status: 30s connect like PHP Poly/Logitech, no proxy, no redirect, trust-all TLS.
	 */
	public static CloseableHttpClient createCodecStatusHttpClient() {
		javax.net.ssl.SSLContext sslContext = null;
		try {
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
		} catch (Exception e) {
		}
		int timeoutMs = 30 * 1000;
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeoutMs)
				.setConnectionRequestTimeout(timeoutMs)
				.setSocketTimeout(timeoutMs)
				.setRedirectsEnabled(false)
				.setCookieSpec(CookieSpecs.STANDARD)
				.build();
		return HttpClientBuilder.create()
				.setDefaultRequestConfig(config)
				.setSSLContext(sslContext)
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.evictExpiredConnections()
				.evictIdleConnections(30, TimeUnit.SECONDS)
				.setMaxConnTotal(100)
				.setMaxConnPerRoute(20)
				.build();
	}
}
