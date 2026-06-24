package com.jdend.erp.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MultiDbProperties.class)
public class DataSourceConfig {

    private final MultiDbProperties properties;

    @Bean
    @Primary
    public DynamicRoutingDataSource dataSource() {
        Map<Object, Object> targetDataSources = new HashMap<>();

        for (Map.Entry<String, MultiDbProperties.DbInfo> entry : properties.getDatasources().entrySet()) {
            targetDataSources.put(entry.getKey(), createDataSource(entry.getValue()));
        }

        DynamicRoutingDataSource routingDataSource =
                new DynamicRoutingDataSource(properties.getDefaultDb());

        Object defaultDs = targetDataSources.get(properties.getDefaultDb());
        routingDataSource.init(targetDataSources, defaultDs);

        return routingDataSource;
    }

    public DataSource createDataSource(MultiDbProperties.DbInfo info) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(info.getUrl());
        ds.setUsername(info.getUsername());
        ds.setPassword(info.getPassword());
        ds.setDriverClassName(info.getDriverClassName());
        return ds;
    }
}