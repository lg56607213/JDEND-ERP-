package com.jdend.erp.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    private final String defaultDb;
    private final Map<Object, Object> dataSources = new ConcurrentHashMap<>();

    public DynamicRoutingDataSource(String defaultDb) {
        this.defaultDb = defaultDb;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String db = TenantContext.getCurrentDb();
        return (db == null || db.isBlank()) ? defaultDb : db;
    }

    public synchronized void init(Map<Object, Object> initialDataSources, Object defaultDataSource) {
        dataSources.clear();
        dataSources.putAll(initialDataSources);

        super.setTargetDataSources(new HashMap<>(dataSources));
        super.setDefaultTargetDataSource(defaultDataSource);
        super.afterPropertiesSet();
    }

    public synchronized void addDataSource(String key, DataSource dataSource) {
        if (key == null || key.isBlank() || dataSource == null) return;

        dataSources.put(key, dataSource);
        super.setTargetDataSources(new HashMap<>(dataSources));

        Object defaultDs = dataSources.get(defaultDb);
        if (defaultDs != null) {
            super.setDefaultTargetDataSource(defaultDs);
        }

        super.afterPropertiesSet();
    }

    public boolean containsDataSource(String key) {
        return dataSources.containsKey(key);
    }
}