package com.jdend.erp.common.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/** 업로드용 엑셀 템플릿(헤더 + 샘플 1행)을 생성한다. */
public final class ExcelTemplateWriter {

  private ExcelTemplateWriter() {}

  public static byte[] write(List<String> headers, List<String> sampleRow) {
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

      if (sampleRow != null && !sampleRow.isEmpty()) {
        Row dataRow = sheet.createRow(1);
        for (int i = 0; i < sampleRow.size(); i++) {
          dataRow.createCell(i).setCellValue(sampleRow.get(i));
        }
      }

      wb.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("엑셀 템플릿 생성 실패: " + e.getMessage());
    }
  }
}
