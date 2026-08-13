package com.jdend.erp.accounting.corporatecard.controller;

import com.jdend.erp.accounting.corporatecard.dto.CorporateCardRowResponse;
import com.jdend.erp.accounting.corporatecard.dto.CorporateCardSaveRequest;
import com.jdend.erp.accounting.corporatecard.dto.CorporateCardSummaryResponse;
import com.jdend.erp.accounting.corporatecard.service.CorporateCardService;
import com.jdend.erp.accounting.voucher.dto.IdListRequest;
import com.jdend.erp.common.excel.ExcelExportService;
import com.jdend.erp.common.excel.ExcelUploadResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/corporate-cards")
@RequiredArgsConstructor
public class CorporateCardController {

    private final CorporateCardService service;
    private final ExcelExportService excelExportService;

    /** GET /api/corporate-cards?startDate=&endDate=&accountCode= (accountCode=__NONE__ → 미분류만) */
    @GetMapping
    public List<CorporateCardRowResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String accountCode) {
        return service.search(startDate, endDate, accountCode);
    }

    /** 조회기간 내 계정분류별 집계 */
    @GetMapping("/summary")
    public List<CorporateCardSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String accountCode) {
        return service.summary(startDate, endDate, accountCode);
    }

    @GetMapping("/upload/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=corporate_card_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(service.template());
    }

    @PostMapping("/upload/preview")
    public ExcelUploadResultResponse preview(@RequestParam("file") MultipartFile file) {
        return service.preview(file);
    }

    @PostMapping("/upload")
    public ExcelUploadResultResponse upload(@RequestParam("file") MultipartFile file) {
        return service.upload(file);
    }

    /** 내역 + 계정분류 일괄 저장 */
    @PutMapping
    public ResponseEntity<?> save(@RequestBody CorporateCardSaveRequest req) {
        try {
            return ResponseEntity.ok(Map.of("saved", service.save(req)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** POST /api/corporate-cards/delete { "ids":[1,2,3] } */
    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody IdListRequest req) {
        try {
            return ResponseEntity.ok(Map.of("deleted", service.delete(req.getIds())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String accountCode) {

        String[] headers = {"사용일자", "카드명", "적요", "금액", "내역", "계정코드", "계정분류"};
        List<Object[]> rows = service.search(startDate, endDate, accountCode).stream()
                .map(r -> new Object[]{
                        r.getUseDate(), r.getCardName(), r.getSummary(), r.getAmount(),
                        r.getDetail(), r.getAccountCode(),
                        (r.getAccountName() == null || r.getAccountName().isBlank()) ? "미분류" : r.getAccountName()
                })
                .toList();

        byte[] data = excelExportService.build("법인카드내역", headers, rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''corporate_card.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
