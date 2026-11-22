package com.caradchecker.springbatch.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

@Configuration
public class DatabaseConfig {

    private final Logger log = LoggerFactory.getLogger(getClass());
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
                    language VARCHAR(2) NOT NULL,
                    country VARCHAR(16) NOT NULL,
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
                    stage VARCHAR(32) NOT NULL,
                    status VARCHAR(16) NOT NULL,
                    result_json LONGTEXT NULL,
                    error_message VARCHAR(2000) NULL,
                    duration_ms BIGINT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_ad_check_extraction_stage (request_id, stage)
                )
                """);
        ensureExtractionStage();
        ensureRequestLanguage();
        ensureRequestCountry();
    }

    private void ensureRequestCountry() {
        Integer columns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'ad_check_requests'
                  AND COLUMN_NAME = 'country'
                """, Integer.class);
        if (columns == null || columns == 0) {
            jdbcTemplate.execute("ALTER TABLE ad_check_requests ADD COLUMN country VARCHAR(16) NULL");
            jdbcTemplate.execute("UPDATE ad_check_requests SET country = 'Romania' WHERE country IS NULL");
            jdbcTemplate.execute("ALTER TABLE ad_check_requests MODIFY country VARCHAR(16) NOT NULL");
        }
    }

    private void ensureRequestLanguage() {
        Integer columns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'ad_check_requests'
                  AND COLUMN_NAME = 'language'
                """, Integer.class);
        if (columns == null || columns == 0) {
            jdbcTemplate.execute("ALTER TABLE ad_check_requests ADD COLUMN language VARCHAR(2) NULL");
            jdbcTemplate.execute("UPDATE ad_check_requests SET language = 'ro' WHERE language IS NULL");
            jdbcTemplate.execute("ALTER TABLE ad_check_requests MODIFY language VARCHAR(2) NOT NULL");
        }
    }

    private void ensureExtractionStage() {
        Integer columns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'ad_check_extractions'
                  AND COLUMN_NAME = 'stage'
                """, Integer.class);
        if (columns == null || columns == 0) {
            jdbcTemplate.execute("ALTER TABLE ad_check_extractions ADD COLUMN stage VARCHAR(32) NULL");
            jdbcTemplate.execute("UPDATE ad_check_extractions SET stage = 'LEGACY' WHERE stage IS NULL");
        }
        jdbcTemplate.execute("ALTER TABLE ad_check_extractions MODIFY stage VARCHAR(32) NOT NULL");
        Integer stageIndex = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'ad_check_extractions'
                  AND INDEX_NAME = 'uk_ad_check_extraction_stage'
                """, Integer.class);
        if (stageIndex != null && stageIndex > 0) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE ad_check_extractions DROP INDEX uk_ad_check_extraction");
        } catch (DataAccessException ex) {
            log.info("Ancien index d'extraction absent: {}", ex.getMostSpecificCause().getMessage());
        }
        jdbcTemplate.execute("ALTER TABLE ad_check_extractions ADD UNIQUE KEY uk_ad_check_extraction_stage (request_id, stage)");
    }

   
}
