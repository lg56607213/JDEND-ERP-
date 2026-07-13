package com.jdend.erp.payment.taxinvoice.service;

import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.customer.Customer;
import com.jdend.erp.customer.CustomerRepository;
import com.jdend.erp.myinfo.entity.SupplierInfo;
import com.jdend.erp.myinfo.repository.SupplierInfoRepository;
import com.jdend.erp.payment.billing.entity.PaymentSchedules;
import com.jdend.erp.payment.billing.repository.PaymentSchedulesRepository;
import com.jdend.erp.payment.taxinvoice.dto.TaxInvoicePreviewRow;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxInvoiceService {

    private final PaymentSchedulesRepository schedulesRepo;
    private final ContractRepository contractRepo;
    private final CustomerRepository customerRepo;
    private final SupplierInfoRepository supplierRepo;

    public List<TaxInvoicePreviewRow> preview(LocalDate taxStart, LocalDate taxEnd,
                                              String type, String customerNumber) {
        List<PaymentSchedules> schedules = fetchSchedules(taxStart, taxEnd, type, customerNumber);
        List<TaxInvoicePreviewRow> result = new ArrayList<>();

        for (PaymentSchedules ps : schedules) {
            String cn = ps.getContractNumber();
            if (cn == null || cn.isBlank()) continue;

            Optional<Contract> contractOpt = contractRepo.findWithCustomerByContractNumber(cn);
            if (contractOpt.isEmpty()) continue;
            Contract contract = contractOpt.get();

            Customer customer = contract.getCustomer();
            if (customer == null && contract.getCustomerNumber() != null) {
                customer = customerRepo.findByCustomerNumber(contract.getCustomerNumber()).orElse(null);
            }

            long supplyAmount = ps.getRentAmount() != null ? ps.getRentAmount() : 0L;
            long taxAmount    = Math.round(supplyAmount * 0.1);

            TaxInvoicePreviewRow row = TaxInvoicePreviewRow.builder()
                    .contractNumber(cn)
                    .installmentNo(ps.getInstallmentNo())
                    .vehicleNo(contract.getVehicleNo())
                    .vehicleModel(contract.getVehicleModel())
                    .taxInvoiceDate(ps.getTaxInvoiceDate())
                    .supplyAmount(supplyAmount)
                    .taxAmount(taxAmount)
                    .build();

            if (customer != null) {
                row.setCustomerName(customer.getCustomerName());
                row.setCeo(firstNonBlank(customer.getCeo(), customer.getCustomerName()));
                row.setRegistrationNumber(customer.getRegistrationNumber());
                row.setAddress(firstNonBlank(customer.getBillAddress(), customer.getAddress()));
                row.setBusinessType(customer.getBusinessType());
                row.setBusinessItem(customer.getBusinessItem());
                row.setEmail(firstNonBlank(customer.getBillEmail(), customer.getManagerEmail()));
            }

            result.add(row);
        }

        return result;
    }

    public byte[] generateExcel(List<TaxInvoicePreviewRow> rows) {
        // 현재 테넌트(회사)의 공급자 정보 조회 — 미입력이면 빈 값으로 안전 처리.
        SupplierInfo supplier = supplierRepo.findTopByOrderByIdAsc().orElse(null);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("엑셀업로드양식");

            // ── 스타일 ──
            CellStyle orangeStyle = buildColorStyle(wb, IndexedColors.LIGHT_ORANGE.getIndex());
            CellStyle greenStyle  = buildColorStyle(wb, IndexedColors.LIGHT_GREEN.getIndex());
            CellStyle numStyle    = wb.createCellStyle();
            numStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0"));

            // ── 1행: 제목 ──
            Row r1 = sheet.createRow(0);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("엑셀 업로드 양식(전자세금계산서-일반(영세율)) - 50건 이하");

            // ── 2~3행: 안내 ──
            sheet.createRow(1).createCell(0).setCellValue("○ 필수항목(주황색)은 반드시 입력하셔야 합니다.");
            sheet.createRow(2).createCell(0).setCellValue("○ 실제 업로드할 DATA는 7행부터 입력하여야 하며, 최대 50건까지 입력이 가능합니다.");

            // ── 4~5행: 빈행 ──
            sheet.createRow(3);
            sheet.createRow(4);

            // ── 6행: 컬럼 헤더 ──
            String[] headers = {
                "전자(세금)계산서 종류\n(01:일반, 02:영세율)",
                "작성일자",
                "공급자 등록번호\n(\"-\" 없이 입력)",
                "공급자\n종사업장번호",
                "공급자 상호",
                "공급자 성명",
                "공급자 사업장주소",
                "공급자 업태",
                "공급자 종목",
                "공급자 이메일",
                "공급받는자 등록번호\n(\"-\" 없이 입력)",
                "공급받는자\n종사업장번호",
                "공급받는자 상호",
                "공급받는자 성명",
                "공급받는자 사업장주소",
                "공급받는자 업태",
                "공급받는자 종목",
                "공급받는자 이메일1",
                "공급받는자 이메일2",
                "공급가액\n합계",
                "세액\n합계",
                "비고",
                "일자1\n(2자리, 작성년월 제외)",
                "품목1",
                "규격1",
                "수량1",
                "단가1",
                "공급가액1",
                "세액1",
                "품목비고1",
                "일자2\n(2자리, 작성년월 제외)",
                "품목2","규격2","수량2","단가2","공급가액2","세액2","품목비고2",
                "일자3\n(2자리, 작성년월 제외)",
                "품목3","규격3","수량3","단가3","공급가액3","세액3","품목비고3",
                "일자4\n(2자리, 작성년월 제외)",
                "품목4","규격4","수량4","단가4","공급가액4","세액4","품목비고4",
                "현금","수표","어음","외상미수금",
                "영수(01),\n청구(02)"
            };

            // 주황색 필수 컬럼 인덱스 (0-based): C1,C2,C3,C5,C6,C11,C13,C14,C20,C21,C23,C28,C29,C59
            boolean[] isOrange = new boolean[59];
            for (int idx : new int[]{0,1,2,4,5,10,12,13,19,20,22,27,28,58}) {
                isOrange[idx] = true;
            }

            Row headerRow = sheet.createRow(5);
            headerRow.setHeightInPoints(50);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(isOrange[i] ? orangeStyle : greenStyle);
                sheet.setColumnWidth(i, 18 * 256);
            }

            // ── 7행~: 데이터 ──
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd");

            for (int i = 0; i < rows.size(); i++) {
                TaxInvoicePreviewRow row = rows.get(i);
                Row dataRow = sheet.createRow(6 + i);

                String[] values = buildRow(row, dateFmt, supplier);
                for (int c = 0; c < values.length; c++) {
                    Cell cell = dataRow.createCell(c);
                    // 금액 컬럼 (C20,C21,C26,C27,C28,C29,C36,C37,C44,C45,C52,C53)
                    if (isNumericCol(c) && !values[c].isEmpty()) {
                        try {
                            cell.setCellValue(Double.parseDouble(values[c]));
                            cell.setCellStyle(numStyle);
                        } catch (NumberFormatException e) {
                            cell.setCellValue(values[c]);
                        }
                    } else {
                        cell.setCellValue(values[c]);
                    }
                }
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("세금계산서 엑셀 생성 실패: " + e.getMessage(), e);
        }
    }

    private String[] buildRow(TaxInvoicePreviewRow row, DateTimeFormatter dateFmt, SupplierInfo supplier) {
        String[] values = new String[59];
        java.util.Arrays.fill(values, "");

        String dateStr  = row.getTaxInvoiceDate() != null ? row.getTaxInvoiceDate().format(dateFmt) : "";
        String day2     = row.getTaxInvoiceDate() != null
                ? String.format("%02d", row.getTaxInvoiceDate().getDayOfMonth()) : "";
        long supply = row.getSupplyAmount() != null ? row.getSupplyAmount() : 0L;
        long tax    = row.getTaxAmount()    != null ? row.getTaxAmount()    : 0L;
        String memo = safe(row.getContractNumber()) + " " + safe(row.getVehicleNo());
        String item = firstNonBlank(row.getVehicleModel(), "차량렌탈");

        // 공급자 (C1~C10) — 현재 테넌트가 '내정보관리'에서 입력한 값 사용(미입력 시 빈 값)
        values[0]  = "01";                                              // 종류
        values[1]  = dateStr;                                           // 작성일자
        values[2]  = supplier != null ? nvl(supplier.getRegistrationNumber()).replace("-", "") : ""; // 공급자 등록번호
        values[3]  = "";                                                // 공급자 종사업장번호
        values[4]  = supplier != null ? nvl(supplier.getCompanyName()) : "";  // 공급자 상호
        values[5]  = supplier != null ? nvl(supplier.getCeoName()) : "";      // 공급자 성명
        values[6]  = supplier != null ? nvl(supplier.getAddress()) : "";      // 공급자 주소
        values[7]  = supplier != null ? nvl(supplier.getBusinessType()) : ""; // 공급자 업태
        values[8]  = supplier != null ? nvl(supplier.getBusinessItem()) : ""; // 공급자 종목
        values[9]  = supplier != null ? nvl(supplier.getEmail()) : "";        // 공급자 이메일

        // 공급받는자 (C11~C19)
        values[10] = nvl(row.getRegistrationNumber()).replace("-", ""); // 등록번호
        values[11] = "";                                 // 종사업장번호
        values[12] = nvl(row.getCustomerName());        // 상호
        values[13] = nvl(row.getCeo());                 // 성명
        values[14] = nvl(row.getAddress());             // 주소
        values[15] = nvl(row.getBusinessType());        // 업태
        values[16] = nvl(row.getBusinessItem());        // 종목
        values[17] = nvl(row.getEmail());               // 이메일1
        values[18] = "";                                 // 이메일2

        // 합계 (C20~C22)
        values[19] = String.valueOf(supply);            // 공급가액 합계
        values[20] = String.valueOf(tax);               // 세액 합계
        values[21] = memo.trim();                       // 비고

        // 품목1 (C23~C30)
        values[22] = day2;                              // 일자1 (2자리)
        values[23] = nvl(item);                        // 품목1
        values[24] = "";                               // 규격1
        values[25] = "1";                              // 수량1
        values[26] = String.valueOf(supply);           // 단가1
        values[27] = String.valueOf(supply);           // 공급가액1
        values[28] = String.valueOf(tax);              // 세액1
        values[29] = "";                               // 품목비고1

        // 품목2~4, 현금~외상미수금: 이미 "" 로 초기화됨 (values[30~57])

        // 영수/청구 (C59)
        values[58] = "02";                             // 청구

        return values;
    }

    private static boolean isNumericCol(int c) {
        // C20,C21,C26,C27,C28,C29 (0-based: 19,20,25,26,27,28)
        // C36,C37,C44,C45,C52,C53 (0-based: 35,36,43,44,51,52)
        // C55~C58 (0-based: 54~57)
        return switch (c) {
            case 19, 20, 25, 26, 27, 28, 35, 36, 43, 44, 51, 52, 54, 55, 56, 57 -> true;
            default -> false;
        };
    }

    private List<PaymentSchedules> fetchSchedules(LocalDate taxStart, LocalDate taxEnd,
                                                  String type, String customerNumber) {
        if ("individual".equalsIgnoreCase(type)
                && customerNumber != null && !customerNumber.isBlank()) {
            List<String> cns = contractRepo.findContractNumbersByCustomerNumber(customerNumber);
            return cns.isEmpty() ? List.of()
                    : schedulesRepo.findByTaxInvoiceDateBetweenAndContractNumbers(taxStart, taxEnd, cns);
        }
        return schedulesRepo.findByTaxInvoiceDateBetween(taxStart, taxEnd);
    }

    private CellStyle buildColorStyle(Workbook wb, short colorIndex) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);
        style.setFillForegroundColor(colorIndex);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private String nvl(String s)   { return s == null ? "" : s; }
    private String safe(String s)  { return s == null ? "" : s.trim(); }

    private String firstNonBlank(String... vals) {
        if (vals == null) return "";
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return "";
    }
}
