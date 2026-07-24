package com.jdend.erp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 키움페이 설정 프로퍼티 등록.
 * DataSourceConfig 패턴과 동일하게 @EnableConfigurationProperties로 바인딩.
 */
@Configuration
@EnableConfigurationProperties(KiwoomPayProperties.class)
public class KiwoomPayConfig {
}
