package com.jdend.erp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.multidb")
public class MultiDbProperties {

    private String defaultDb = "auth";

    private String templateDb = "erp";

    private Map<String, DbInfo> datasources = new HashMap<>();

    private Pool pool = new Pool();

    /**
     * 커넥션 풀 크기. 회사(테넌트)마다 별도 풀이 생기므로 기본값(최대 10, 유휴 10)을
     * 그대로 쓰면 회사 수 × 10 만큼 MySQL 커넥션을 상시 점유해
     * max_connections(기본 151)에 금방 도달한다.
     */
    @Getter
    @Setter
    public static class Pool {
        /** 인증/공통 DB 풀 최대 커넥션 */
        private int authMaxSize = 5;
        /** 인증/공통 DB 풀 최소 유휴 커넥션 */
        private int authMinIdle = 1;
        /** 회사별 DB 풀 최대 커넥션 */
        private int tenantMaxSize = 3;
        /** 회사별 DB 풀 최소 유휴 커넥션 (0이면 미사용 회사는 커넥션을 반납) */
        private int tenantMinIdle = 0;
        /** 유휴 커넥션 반납까지의 시간(ms) */
        private long idleTimeoutMs = 60_000L;
        /** 커넥션 획득 대기 제한(ms) */
        private long connectionTimeoutMs = 10_000L;
        /** 커넥션 최대 수명(ms) */
        private long maxLifetimeMs = 1_500_000L;
    }

    @Getter
    @Setter
    public static class DbInfo {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
    }
}