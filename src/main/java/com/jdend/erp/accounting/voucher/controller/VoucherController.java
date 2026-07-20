package com.jdend.erp.accounting.voucher.controller;

import com.jdend.erp.accounting.voucher.dto.*;
import com.jdend.erp.accounting.voucher.service.VoucherBulkUploadService;
import com.jdend.erp.accounting.voucher.service.VoucherService;
import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.common.excel.ExcelExportService;
import com.jdend.erp.common.excel.ExcelUploadResultResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vouchers")

public class VoucherController {

    private final VoucherService voucherService;
    private final VoucherBulkUploadService bulkUploadService;
    private final PermissionService permissionService;
    private final ExcelExportService excelExportService;

    /** GET /api/vouchers/next-no?date=2026-03-02 */
    @GetMapping("/next-no")
    public VoucherNextNoResponse nextNo(@RequestParam("date") LocalDate date) {
        return VoucherNextNoResponse.builder()
                .voucherNo(voucherService.nextVoucherNo(date))
                .build();
    }

    /** POST /api/vouchers */
    @PostMapping
    public VoucherCreateResponse create(@RequestBody VoucherCreateRequest req, HttpSession session) {
        permissionService.requireManager(session);
        return voucherService.create(req);
    }

    // ==========================
    // ✅ 전표승인 화면 API
    // ==========================

    // GET /api/vouchers?date=2026-03-03&status=대기
    @GetMapping
    public List<VoucherApprovalRowResponse> list(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false, defaultValue = "") String status
    ) {
        return voucherService.listForApproval(date, status);
    }

    // POST /api/vouchers/approve  { "ids":[1,2,3] }
    @PostMapping("/approve")
    public BulkResultResponse approve(@RequestBody IdListRequest req, HttpSession session) {
        permissionService.requireManager(session);
        int affected = voucherService.approveByIds(req.getIds());
        return BulkResultResponse.builder().affected(affected).build();
    }

    // POST /api/vouchers/delete  { "ids":[1,2,3] }
    @PostMapping("/delete")
    public BulkResultResponse delete(@RequestBody IdListRequest req, HttpSession session) {
        permissionService.requireManager(session);
        int affected = voucherService.deleteByIds(req.getIds());
        return BulkResultResponse.builder().affected(affected).build();
    }

    @GetMapping("/bulk-upload/template")
    public ResponseEntity<byte[]> bulkUploadTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=voucher_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bulkUploadService.template());
    }

    @PostMapping("/bulk-upload")
    public ExcelUploadResultResponse bulkUpload(@RequestParam("file") MultipartFile file) {
        return bulkUploadService.upload(file);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false, defaultValue = "") String status
    ) {
        String[] headers = {"전표일자", "전표번호", "차변계정", "차변금액", "차변적요",
                "대변계정", "대변금액", "대변적요", "상태"};
        java.util.List<Object[]> rows = voucherService.listForApproval(date, status).stream()
                .filter(VoucherApprovalRowResponse::isShowMain)
                .map(v -> new Object[]{
                        v.getVoucherDate(), v.getVoucherNo(),
                        v.getDebitAccount(), v.getDebitAmount(), v.getDebitDescription(),
                        v.getCreditAccount(), v.getCreditAmount(), v.getCreditDescription(),
                        v.getStatus()
                }).collect(java.util.stream.Collectors.toList());
        byte[] data = excelExportService.build("전표목록", headers, rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''vouchers.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}