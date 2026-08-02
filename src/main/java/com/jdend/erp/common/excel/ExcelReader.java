package com.jdend.erp.common.excel;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 업로드된 xlsx를 SAX 스트리밍 방식으로 파싱한다.
 * DOM 방식(XSSFWorkbook)은 파일 크기의 수십 배 힙을 사용해 OOM이 발생하므로,
 * 행 단위로 처리하는 이벤트 기반 API를 사용한다.
 */
public final class ExcelReader {

    private ExcelReader() {}

    public static List<Map<String, String>> readRows(InputStream is) {
        Path tmp = null;
        try {
            // OPCPackage는 seekable 스트림이 필요하므로 임시파일에 먼저 기록
            tmp = Files.createTempFile("erp_excel_", ".xlsx");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                is.transferTo(out);
            }

            try (OPCPackage pkg = OPCPackage.open(tmp.toFile())) {
                XSSFReader xssfReader       = new XSSFReader(pkg);
                ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
                XSSFReader.SheetIterator sheets =
                        (XSSFReader.SheetIterator) xssfReader.getSheetsData();

                RowCollector handler = new RowCollector();

                if (sheets.hasNext()) {
                    try (InputStream sheetStream = sheets.next()) {
                        XMLReader xmlReader     = XMLHelper.newXMLReader();
                        ContentHandler content  = new XSSFSheetXMLHandler(
                                xssfReader.getStylesTable(), strings, handler, false);
                        xmlReader.setContentHandler(content);
                        xmlReader.parse(new InputSource(sheetStream));
                    }
                }
                return handler.getRows();
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("엑셀 파일을 읽을 수 없습니다: " + e.getMessage());
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            }
        }
    }

    private static final class RowCollector implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final List<Map<String, String>> rows = new ArrayList<>();
        private List<String> headers = null;
        private final Map<Integer, String> currentCells = new TreeMap<>();

        @Override
        public void startRow(int rowNum) {
            currentCells.clear();
        }

        @Override
        public void endRow(int rowNum) {
            if (rowNum == 0) {
                int maxCol = currentCells.isEmpty() ? 0
                        : Collections.max(currentCells.keySet()) + 1;
                headers = new ArrayList<>(Collections.nCopies(maxCol, ""));
                currentCells.forEach((col, val) -> {
                    if (col < headers.size()) headers.set(col, val.trim());
                });
            } else if (headers != null) {
                boolean blank = currentCells.values().stream().allMatch(String::isBlank);
                if (!blank) {
                    Map<String, String> map = new LinkedHashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        String h = headers.get(i);
                        if (!h.isBlank()) {
                            map.put(h, currentCells.getOrDefault(i, "").trim());
                        }
                    }
                    rows.add(map);
                }
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            if (formattedValue == null || formattedValue.isBlank()) return;
            CellReference ref = new CellReference(cellReference);
            currentCells.put((int) ref.getCol(), formattedValue);
        }

        public List<Map<String, String>> getRows() { return rows; }
    }
}
