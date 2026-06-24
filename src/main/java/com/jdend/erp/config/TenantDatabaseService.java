package com.jdend.erp.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantDatabaseService {

    private final JdbcTemplate jdbcTemplate;
    private final DynamicRoutingDataSource routingDataSource;
    private final MultiDbProperties properties;

    public String normalizeTenantDb(String input, String loginId) {
        String raw = input == null || input.isBlank() ? loginId : input;

        String cleaned = raw.trim().toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_");

        if (cleaned.isBlank()) {
            throw new RuntimeException("회사코드/DB 값이 올바르지 않습니다.");
        }

        if (cleaned.startsWith("erp_company_")) {
            return cleaned;
        }

        return "erp_company_" + cleaned;
    }

    public void ensureTenantDatabase(String targetDb) {
        validateDbName(targetDb);
        createDatabaseIfNotExists(targetDb);
        copySchemaIfEmpty(targetDb);
        registerDataSourceIfMissing(targetDb);
    }

    private void createDatabaseIfNotExists(String targetDb) {
        jdbcTemplate.execute(
                "CREATE DATABASE IF NOT EXISTS `" + targetDb + "` " +
                "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
        );
    }

    private void copySchemaIfEmpty(String targetDb) {
        String templateDb = properties.getTemplateDb();

        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_type = 'BASE TABLE'
                """,
                Integer.class,
                targetDb
        );

        if (tableCount != null && tableCount > 0) {
            return;
        }

        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """,
                String.class,
                templateDb
        );

        for (String table : tables) {
            if ("login_users".equalsIgnoreCase(table)) {
                continue;
            }

            jdbcTemplate.execute(
                    "CREATE TABLE `" + targetDb + "`.`" + table + "` " +
                    "LIKE `" + templateDb + "`.`" + table + "`"
            );
        }
    }

    private void registerDataSourceIfMissing(String targetDb) {
        if (routingDataSource.containsDataSource(targetDb)) {
            return;
        }

        MultiDbProperties.DbInfo auth = properties.getDatasources().get(properties.getDefaultDb());

        if (auth == null) {
            throw new RuntimeException("기본 DB 설정(auth)을 찾을 수 없습니다.");
        }

        String tenantUrl = replaceDbName(auth.getUrl(), targetDb);

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(tenantUrl);
        ds.setUsername(auth.getUsername());
        ds.setPassword(auth.getPassword());
        ds.setDriverClassName(auth.getDriverClassName());

        routingDataSource.addDataSource(targetDb, ds);
    }

    private String replaceDbName(String url, String targetDb) {
        return url.replaceFirst(
                "(jdbc:mysql://[^/]+/)([^?]+)(.*)",
                "$1" + targetDb + "$3"
        );
    }

    private void validateDbName(String dbName) {
        if (dbName == null || !dbName.matches("^[a-zA-Z0-9_]+$")) {
            throw new RuntimeException("DB 이름은 영문/숫자/언더바만 가능합니다.");
        }
    }
}