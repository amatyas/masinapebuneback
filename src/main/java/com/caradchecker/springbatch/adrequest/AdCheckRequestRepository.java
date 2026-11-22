package com.caradchecker.springbatch.adrequest;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdCheckRequestRepository extends JpaRepository<AdCheckRequest, String> {

	List<AdCheckRequest> findByUrlAndLanguageAndCountryOrderByCreatedAtDesc(String url, String language, String country);
}
