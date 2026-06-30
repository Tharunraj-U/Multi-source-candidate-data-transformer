package com.eightfold.candidate.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitVerifier {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void verify() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1",
                Integer.class);

        List<Map<String, Object>> tables = jdbcTemplate.queryForList("""
                SELECT TABLE_NAME
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_TYPE = 'BASE TABLE'
                  AND TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME NOT LIKE 'flyway_%'
                ORDER BY TABLE_NAME
                """);

        Integer skillAliasCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM skill_alias", Integer.class);

        log.info("Database initialization OK — Flyway migrations applied: {}, tables: {}, skill_alias seed rows: {}",
                migrationCount, tables.size(), skillAliasCount);
        tables.forEach(row -> log.info("  - {}", row.get("TABLE_NAME")));
    }
}
