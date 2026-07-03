package com.jdend.erp.common.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 엑셀 다운로드용 공통 서비스.
 * headers + rows(Object[]) 를 받아 .xlsx 바이트 배열을 반환한다.
 */
@Service
public class ExcelExportService {

    public byte[] build(String sheetName, String[] headers, List<Object[]> rows) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);

            // 헤더 스타일
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // 숫자 스타일
            CellStyle numStyle = wb.createCellStyle();
            DataFormat fmt = wb.createDataFormat();
            numStyle.setDataFormat(fmt.getFormat("#,##0"));
            numStyle.setAlignment(HorizontalAlignment.RIGHT);

            // 날짜 스타일
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(fmt.getFormat("yyyy-mm-dd"));

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 18 * 256);
            }

            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                Object[] values = rows.get(r);
                for (int c = 0; c < values.length; c++) {
                    Cell cell = row.createCell(c);
                    Object val = values[c];
                    if (val == null) {
                        cell.setCellValue("");
                    } else if (val instanceof Number) {
                        cell.setCellValue(((Number) val).doubleValue());
                        cell.setCellStyle(numStyle);
                    } else if (val instanceof LocalDate) {
                        cell.setCellValue(val.toString());
                        cell.setCellStyle(dateStyle);
                    } else if (val instanceof LocalDateTime) {
                        cell.setCellValue(((LocalDateTime) val).toLocalDate().toString());
                    } else {
                        cell.setCellValue(val.toString());
                    }
                }
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("엑셀 생성 실패: " + e.getMessage(), e);
        }
    }
}
