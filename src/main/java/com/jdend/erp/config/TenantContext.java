package com.jdend.erp.config;

public class TenantContext {

    private static final ThreadLocal<String> CURRENT_DB = new ThreadLocal<>();

    public static void setCurrentDb(String db) {
        CURRENT_DB.set(db);
    }

    public static String getCurrentDb() {
        return CURRENT_DB.get();
    }

    public static void clear() {
        CURRENT_DB.remove();
    }
}