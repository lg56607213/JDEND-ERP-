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

    @Getter
    @Setter
    public static class DbInfo {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
    }
}