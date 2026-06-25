package com.jdend.erp.common.excel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** 엑셀 한 행(헤더명 → 문자열값 Map)에서 타입별 값을 꺼내는 공통 헬퍼. */
public final class ExcelRowParsers {

  private ExcelRowParsers() {}

  public static String str(Map<String, String> row, String key) {
    String v = row.get(key);
    return (v == null || v.isBlank()) ? null : v.trim();
  }

  public static Long longVal(Map<String, String> row, String key) {
    String v = str(row, key);
    if (v == null) return null;
    try {
      return Long.parseLong(v.replace(",", "").trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("'" + key + "' 값이 올바른 숫자가 아닙니다: " + v);
    }
  }

  public static Integer intVal(Map<String, String> row, String key) {
    Long v = longVal(row, key);
    return v == null ? null : v.intValue();
  }

  public static LocalDate dateVal(Map<String, String> row, String key) {
    String v = str(row, key);
    if (v == null) return null;
    try {
      return LocalDate.parse(v.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
    } catch (Exception e) {
      throw new IllegalArgumentException("'" + key + "' 값이 올바른 날짜 형식(yyyy-MM-dd)이 아닙니다: " + v);
    }
  }
}
