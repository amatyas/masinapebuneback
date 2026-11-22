package com.caradchecker.springbatch.rest.api.adrequest;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.caradchecker.springbatch.adrequest.AdCheckRequest;
import com.caradchecker.springbatch.adrequest.AdRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/requests")
public class AdRequestController {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final AdRequestService adRequestService;
	private final ObjectMapper objectMapper;

	public AdRequestController(AdRequestService adRequestService, ObjectMapper objectMapper) {
		this.adRequestService = adRequestService;
		this.objectMapper = objectMapper;
	}

	@PostMapping
	public ResponseEntity<AdRequestResponse> create(@RequestBody CreateAdRequest body) {
		String requestId = UUID.randomUUID().toString();
		long start = System.nanoTime();
		log.info("{} incoming request url={}", requestId, body == null ? null : body.url());
		try {
			AdCheckRequest created = adRequestService.create(requestId, body == null ? null : body.url());
			return ResponseEntity.status(HttpStatus.CREATED).body(AdRequestResponse.from(created, List.of(), objectMapper));
		} finally {
			log.info("{} total execution time {} ms", requestId, elapsedMillis(start));
		}
	}

	@GetMapping("/{requestId}")
	public AdRequestResponse get(@PathVariable String requestId) {
		long start = System.nanoTime();
		log.info("{} incoming request", requestId);
		try {
			AdCheckRequest request = adRequestService.get(requestId);
			return AdRequestResponse.from(request, adRequestService.extractions(request.getId()), objectMapper);
		} finally {
			log.info("{} total execution time {} ms", requestId, elapsedMillis(start));
		}
	}

	private long elapsedMillis(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000L;
	}
}
