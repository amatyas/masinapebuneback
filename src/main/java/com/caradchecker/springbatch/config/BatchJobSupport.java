package com.caradchecker.springbatch.config;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

/**
 * Shared Spring Batch helpers (startup skip, long-running tasklet transaction).
 */
public final class BatchJobSupport {

	private BatchJobSupport() {
	}

	public static boolean isStartupMainThread() {
		return "main".equalsIgnoreCase(Thread.currentThread().getName());
	}

	public static DefaultTransactionAttribute notSupportedTransaction() {
		DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
		attribute.setPropagationBehavior(Propagation.NOT_SUPPORTED.value());
		attribute.setIsolationLevel(Isolation.DEFAULT.value());
		return attribute;
	}
}
