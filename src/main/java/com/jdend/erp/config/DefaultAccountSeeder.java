package com.jdend.erp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 앱 시작 시 template DB(erp)의 financial_statement_accounts가 비어있거나
 * 필수 계정이 누락된 경우 seed SQL을 실행해 자동으로 채운다.
 *
 * seed SQL이 INSERT IGNORE + UPDATE 로 구성되어 있어 중복 실행해도 안전하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAccountSeeder implements ApplicationRunner {

    private final DataSource dataSource;
    private final MultiDbProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ClassPathResource resource = new ClassPathResource("sql/seed_financial_statement_accounts_fresh.sql");
            if (!resource.exists()) {
                log.warn("[DefaultAccountSeeder] seed SQL 파일 없음: sql/seed_financial_statement_accounts_fresh.sql");
                return;
            }
            try (Connection conn = dataSource.getConnection()) {
                String original = conn.getCatalog();
                conn.setCatalog(properties.getTemplateDb());
                try {
                    ScriptUtils.executeSqlScript(conn, resource);
                } finally {
                    if (original != null) conn.setCatalog(original);
                }
            }
            log.info("[DefaultAccountSeeder] template DB({}) 재무제표 기본 계정 시딩 완료", properties.getTemplateDb());
        } catch (Exception e) {
            log.error("[DefaultAccountSeeder] 재무제표 기본 계정 시딩 실패: {}", e.getMessage(), e);
        }
    }
}
