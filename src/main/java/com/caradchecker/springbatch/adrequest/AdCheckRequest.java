package com.caradchecker.springbatch.adrequest;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_check_requests")
public class AdCheckRequest {

	@Id
	@Column(name = "id", length = 36, nullable = false, updatable = false)
	private String id;

	@Column(name = "url", nullable = false, length = 2048)
	private String url;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private AdCheckStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "language", nullable = false, length = 2)
	private String language;

	@Column(name = "country", nullable = false, length = 16)
	private String country;

	protected AdCheckRequest() {
	}

	public AdCheckRequest(String id, String url, String language, String country) {
		this.id = id;
		this.url = url;
		this.language = language;
		this.country = country;
		this.status = AdCheckStatus.PENDING;
		this.createdAt = LocalDateTime.now();
	}

	public String getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

	public AdCheckStatus getStatus() {
		return status;
	}

	public void setStatus(AdCheckStatus status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public String getLanguage() {
		return language;
	}

	public String getCountry() {
		return country;
	}
}
