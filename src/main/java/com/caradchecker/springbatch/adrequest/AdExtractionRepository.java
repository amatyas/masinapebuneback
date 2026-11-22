package com.caradchecker.springbatch.adrequest;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdExtractionRepository extends JpaRepository<AdExtraction, String> {

	List<AdExtraction> findByRequestId(String requestId);

	Optional<AdExtraction> findFirstByRequestIdAndStageAndStatusOrderByCreatedAtDesc(String requestId, String stage, ExtractionStatus status);
}
