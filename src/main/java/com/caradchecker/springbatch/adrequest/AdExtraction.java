package com.caradchecker.springbatch.adrequest;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
	@Table(name = "ad_check_extractions", uniqueConstraints = @UniqueConstraint(columnNames = { "request_id", "stage" }))
public class AdExtraction {

	@Id
	@Column(name = "id", length = 36, nullable = false, updatable = false)
	private String id;

	@Column(name = "request_id", length = 36, nullable = false, updatable = false)
	private String requestId;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", length = 16, nullable = false, updatable = false)
	private ExtractionProvider provider;

	@Column(name = "stage", length = 32, nullable = false, updatable = false)
	private String stage;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 16, nullable = false)
	private ExtractionStatus status;

	@Column(name = "result_json", columnDefinition = "LONGTEXT")
	private String resultJson;

	@Column(name = "error_message", length = 2000)
	private String errorMessage;

	@Column(name = "duration_ms")
	private Long durationMs;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected AdExtraction() {
	}

	public AdExtraction(String requestId, ExtractionProvider provider, String stage, ExtractionStatus status, String resultJson,
			String errorMessage, long durationMs) {
		this.id = UUID.randomUUID().toString();
		this.requestId = requestId;
		this.provider = provider;
		this.stage = stage;
		this.status = status;
		this.resultJson = resultJson;
		this.errorMessage = errorMessage;
		this.durationMs = durationMs;
		this.createdAt = LocalDateTime.now();
	}

	public String getRequestId() {
		return requestId;
	}

	public ExtractionProvider getProvider() {
		return provider;
	}

	public String getStage() {
		return stage;
	}

	public ExtractionStatus getStatus() {
		return status;
	}

	public String getResultJson() {
		return resultJson;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Long getDurationMs() {
		return durationMs;
	}
}
