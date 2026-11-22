package com.caradchecker.springbatch.rest.api.adrequest;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class AdRequestEvents {

	private static final long TIMEOUT_MS = 15 * 60 * 1000L;

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

	public SseEmitter subscribe(String requestId, Supplier<AdRequestResponse> snapshot) {
		SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
		CopyOnWriteArrayList<SseEmitter> subscribers = emitters.computeIfAbsent(requestId, id -> new CopyOnWriteArrayList<>());
		synchronized (lock(requestId)) {
			subscribers.add(emitter);
			emitter.onCompletion(() -> remove(requestId, emitter));
			emitter.onTimeout(() -> remove(requestId, emitter));
			emitter.onError(ex -> remove(requestId, emitter));
			send(requestId, emitter, snapshot.get());
		}
		return emitter;
	}

	public void publish(String requestId, Supplier<AdRequestResponse> snapshot) {
		CopyOnWriteArrayList<SseEmitter> subscribers = emitters.get(requestId);
		if (subscribers == null || subscribers.isEmpty()) {
			return;
		}
		synchronized (lock(requestId)) {
			subscribers = emitters.get(requestId);
			if (subscribers == null || subscribers.isEmpty()) {
				return;
			}
			AdRequestResponse body = snapshot.get();
			for (SseEmitter emitter : subscribers) {
				send(requestId, emitter, body);
			}
		}
	}

	@Scheduled(fixedRate = 15000)
	public void heartbeat() {
		for (String requestId : emitters.keySet()) {
			CopyOnWriteArrayList<SseEmitter> subscribers = emitters.get(requestId);
			if (subscribers == null || subscribers.isEmpty()) {
				continue;
			}
			synchronized (lock(requestId)) {
				for (SseEmitter emitter : List.copyOf(subscribers)) {
					try {
						emitter.send(SseEmitter.event().comment("ping"));
					} catch (Exception ex) {
						close(requestId, emitter);
					}
				}
			}
		}
	}

	private void send(String requestId, SseEmitter emitter, AdRequestResponse body) {
		if (body == null) {
			return;
		}
		try {
			emitter.send(SseEmitter.event().name("request").data(body, MediaType.APPLICATION_JSON));
			if (isTerminal(body.status())) {
				emitter.complete();
			}
		} catch (Exception ex) {
			log.info("{} event stream closed: {}", requestId, ex.getMessage());
			close(requestId, emitter);
		}
	}

	private void close(String requestId, SseEmitter emitter) {
		try {
			emitter.complete();
		} catch (Exception ex) {
			log.debug("{} event stream already closed", requestId);
		}
		remove(requestId, emitter);
	}

	private void remove(String requestId, SseEmitter emitter) {
		CopyOnWriteArrayList<SseEmitter> subscribers = emitters.get(requestId);
		if (subscribers == null) {
			return;
		}
		subscribers.remove(emitter);
		if (subscribers.isEmpty()) {
			emitters.remove(requestId, subscribers);
		}
	}

	private Object lock(String requestId) {
		return locks.computeIfAbsent(requestId, id -> new Object());
	}

	private boolean isTerminal(String status) {
		return "COMPLETED".equals(status) || "FAILED".equals(status);
	}
}
