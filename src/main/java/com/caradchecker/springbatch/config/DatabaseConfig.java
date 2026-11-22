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

    }

   
}
