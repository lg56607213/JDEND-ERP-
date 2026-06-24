package com.jdend.erp.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        String db = TenantContext.getCurrentDb();

        if (db == null || db.isBlank()) {
            return "auth";
        }

        return db;
    }
}