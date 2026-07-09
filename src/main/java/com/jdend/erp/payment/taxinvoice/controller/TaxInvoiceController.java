package com.jdend.erp.payment.taxinvoice.controller;

import com.jdend.erp.payment.taxinvoice.dto.TaxInvoicePreviewRow;
import com.jdend.erp.payment.taxinvoice.service.TaxInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tax-invoices")
public class TaxInvoiceController {

    private static final int PAGE_SIZE = 50;

    private final TaxInvoiceService service;

    /** 미리보기: 전체 목록 반환 */
    @GetMapping("/preview")
    public List<TaxInvoicePreviewRow> preview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate taxStartDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate taxEndDate,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false) String customerNumber
    ) {
        return service.preview(taxStartDate, taxEndDate, type, customerNumber);
    }

    /** 엑셀 다운로드: page 번호(0부터)로 50건씩 */
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate taxStartDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate taxEndDate,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false) String customerNumber,
            @RequestParam(defaultValue = "0") int page
    ) {
        List<TaxInvoicePreviewRow> all = service.preview(taxStartDate, taxEndDate, type, customerNumber);

        int from = page * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, all.size());

        if (from >= all.size()) {
            return ResponseEntity.noContent().build();
        }

        List<TaxInvoicePreviewRow> pageRows = all.subList(from, to);
        byte[] excel = service.generateExcel(pageRows);

        String filename = String.format("세금계산서_%s_%s_p%d.xlsx",
                taxStartDate, taxEndDate, page + 1);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }
}
