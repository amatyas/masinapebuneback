package com.caradchecker.springbatch.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

@Configuration
public class DatabaseConfig {


    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseConfig(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    @Transactional
    public void initialize() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ad_check_requests (
                    id VARCHAR(36) NOT NULL,
                    url VARCHAR(2048) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ad_check_extractions (
                    id VARCHAR(36) NOT NULL,
                    request_id VARCHAR(36) NOT NULL,
                    provider VARCHAR(16) NOT NULL,
                    status VARCHAR(16) NOT NULL,
                    result_json LONGTEXT NULL,
                    error_message VARCHAR(2000) NULL,
                    duration_ms BIGINT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_ad_check_extraction (request_id, provider)
                )
                """);
    }

   
}
