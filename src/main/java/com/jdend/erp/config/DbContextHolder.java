package com.jdend.erp.config;

public class DbContextHolder {

  private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

  public static void set(String dbKey) {
    CONTEXT.set(dbKey);
  }

  public static String get() {
    return CONTEXT.get();
  }

  public static void clear() {
    CONTEXT.remove();
  }
}