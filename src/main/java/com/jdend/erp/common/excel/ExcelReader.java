package com.jdend.erp.common.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 업로드된 xlsx의 첫 번째 시트를 읽어 1행을 헤더로 사용하고,
 * 이후 각 행을 헤더명 → 셀값(문자열) Map으로 변환한다.
 */
public final class ExcelReader {

  private ExcelReader() {}

  public static List<Map<String, String>> readRows(InputStream is) {
    try (Workbook wb = new XSSFWorkbook(is)) {
      Sheet sheet = wb.getSheetAt(0);
      List<Map<String, String>> rows = new ArrayList<>();
      if (sheet == null) return rows;

      Row headerRow = sheet.getRow(0);
      if (headerRow == null) return rows;

      List<String> headers = new ArrayList<>();
      for (Cell cell : headerRow) {
        headers.add(cellToString(cell).trim());
      }

      int lastRow = sheet.getLastRowNum();
      for (int r = 1; r <= lastRow; r++) {
        Row row = sheet.getRow(r);
        if (row == null || isRowBlank(row)) continue;

        Map<String, String> map = new LinkedHashMap<>();
        for (int c = 0; c < headers.size(); c++) {
          String header = headers.get(c);
          if (header.isBlank()) continue;
          map.put(header, cellToString(row.getCell(c)).trim());
        }
        rows.add(map);
      }
      return rows;
    } catch (Exception e) {
      throw new IllegalArgumentException("엑셀 파일을 읽을 수 없습니다: " + e.getMessage());
    }
  }

  private static boolean isRowBlank(Row row) {
    for (Cell cell : row) {
      if (!cellToString(cell).isBlank()) return false;
    }
    return true;
  }

  private static String cellToString(Cell cell) {
    if (cell == null) return "";

    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case NUMERIC -> {
        if (DateUtil.isCellDateFormatted(cell)) {
          yield cell.getLocalDateTimeCellValue().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        double val = cell.getNumericCellValue();
        if (val == Math.floor(val) && !Double.isInfinite(val)) {
          yield String.valueOf((long) val);
        }
        yield String.valueOf(val);
      }
      case FORMULA -> {
        try {
          yield cell.getStringCellValue();
        } catch (Exception e) {
          try {
            yield String.valueOf(cell.getNumericCellValue());
          } catch (Exception e2) {
            yield "";
          }
        }
      }
      default -> "";
    };
  }
}
