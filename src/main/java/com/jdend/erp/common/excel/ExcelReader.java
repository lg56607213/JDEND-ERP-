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

    /** 첫 번째 행을 헤더로 간주하고 읽는다. */
    public static List<Map<String, String>> readRows(InputStream is) {
        return readRows(is, List.of());
    }

    /**
     * expectedHeaders 중 2개 이상이 들어있는 첫 행을 헤더로 간주하고 읽는다.
     * 은행 홈페이지에서 받은 파일처럼 헤더 위에 제목/계좌정보 행이 붙어 있어도 인식된다.
     * expectedHeaders가 비어 있으면 첫 번째 행을 헤더로 사용한다(기존 동작).
     */
    public static List<Map<String, String>> readRows(InputStream is, List<String> expectedHeaders) {
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

                RowCollector handler = new RowCollector(expectedHeaders);

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
        private final List<String> expected;
        /** 원본 행: (엑셀 행번호, 열번호 → 값) */
        private final List<Map.Entry<Integer, Map<Integer, String>>> rawRows = new ArrayList<>();
        private final Map<Integer, String> currentCells = new TreeMap<>();

        private RowCollector(List<String> expectedHeaders) {
            this.expected = expectedHeaders == null ? List.of() : expectedHeaders;
        }

        @Override
        public void startRow(int rowNum) {
            currentCells.clear();
        }

        @Override
        public void endRow(int rowNum) {
            rawRows.add(Map.entry(rowNum, new TreeMap<>(currentCells)));
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            if (formattedValue == null || formattedValue.isBlank()) return;
            CellReference ref = new CellReference(cellReference);
            currentCells.put((int) ref.getCol(), formattedValue);
        }

        public List<Map<String, String>> getRows() {
            int headerIdx = findHeaderIndex();
            List<Map<String, String>> rows = new ArrayList<>();
            if (headerIdx < 0) return rows;

            List<String> headers = toHeaders(rawRows.get(headerIdx).getValue());

            for (int i = headerIdx + 1; i < rawRows.size(); i++) {
                Map<Integer, String> cells = rawRows.get(i).getValue();
                if (cells.values().stream().allMatch(String::isBlank)) continue;

                Map<String, String> map = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    String h = headers.get(c);
                    if (!h.isBlank()) {
                        map.put(h, cells.getOrDefault(c, "").trim());
                    }
                }
                if (!map.isEmpty()) rows.add(map);
            }
            return rows;
        }

        /** expected가 있으면 해당 헤더가 2개 이상 있는 첫 행, 없으면 엑셀 0번 행 */
        private int findHeaderIndex() {
            if (rawRows.isEmpty()) return -1;

            if (!expected.isEmpty()) {
                Set<String> wanted = new HashSet<>();
                expected.forEach(h -> wanted.add(norm(h)));
                for (int i = 0; i < rawRows.size(); i++) {
                    long hit = toHeaders(rawRows.get(i).getValue()).stream()
                            .map(RowCollector::norm)
                            .filter(wanted::contains)
                            .distinct()
                            .count();
                    if (hit >= 2) return i;
                }
            }
            for (int i = 0; i < rawRows.size(); i++) {
                if (rawRows.get(i).getKey() == 0) return i;
            }
            return expected.isEmpty() ? -1 : 0;
        }

        /** 헤더 비교용 정규화: 공백 제거 + 대문자화 */
        private static String norm(String s) {
            return s == null ? "" : s.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        }

        private List<String> toHeaders(Map<Integer, String> cells) {
            int maxCol = cells.isEmpty() ? 0 : Collections.max(cells.keySet()) + 1;
            List<String> headers = new ArrayList<>(Collections.nCopies(maxCol, ""));
            cells.forEach((col, val) -> {
                if (col < headers.size()) headers.set(col, val.trim());
            });
            return headers;
        }
    }
}
