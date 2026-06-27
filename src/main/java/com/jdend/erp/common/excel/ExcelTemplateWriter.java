package com.jdend.erp.common.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/** 업로드용 엑셀 템플릿(헤더 + 샘플 행)을 생성한다. */
public final class ExcelTemplateWriter {

  private ExcelTemplateWriter() {}

  public static byte[] write(List<String> headers, List<String> sampleRow) {
    return writeMultiRow(headers, sampleRow == null ? List.of() : List.of(sampleRow));
  }

  public static byte[] writeMultiRow(List<String> headers, List<List<String>> sampleRows) {
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("양식");

      CellStyle headerStyle = wb.createCellStyle();
      Font headerFont = wb.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.size(); i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers.get(i));
        cell.setCellStyle(headerStyle);
        sheet.setColumnWidth(i, 18 * 256);
      }

      if (sampleRows != null) {
        for (int r = 0; r < sampleRows.size(); r++) {
          List<String> sampleRow = sampleRows.get(r);
          if (sampleRow == null) continue;
          Row dataRow = sheet.createRow(r + 1);
          for (int i = 0; i < sampleRow.size(); i++) {
            if (sampleRow.get(i) != null) {
              dataRow.createCell(i).setCellValue(sampleRow.get(i));
            }
          }
        }
      }

      wb.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("엑셀 템플릿 생성 실패: " + e.getMessage());
    }
  }
}
