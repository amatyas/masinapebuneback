package com.caradchecker.springbatch.adrequest;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdExtractionRepository extends JpaRepository<AdExtraction, String> {

	List<AdExtraction> findByRequestId(String requestId);
}
