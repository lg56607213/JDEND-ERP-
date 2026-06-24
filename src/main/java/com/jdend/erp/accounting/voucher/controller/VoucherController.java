package com.jdend.erp.accounting.voucher.controller;

import com.jdend.erp.accounting.voucher.dto.*;
import com.jdend.erp.accounting.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vouchers")

public class VoucherController {

    private final VoucherService voucherService;

    /** GET /api/vouchers/next-no?date=2026-03-02 */
    @GetMapping("/next-no")
    public VoucherNextNoResponse nextNo(@RequestParam("date") LocalDate date) {
        return VoucherNextNoResponse.builder()
                .voucherNo(voucherService.nextVoucherNo(date))
                .build();
    }

    /** POST /api/vouchers */
    @PostMapping
    public VoucherCreateResponse create(@RequestBody VoucherCreateRequest req) {
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
    public BulkResultResponse approve(@RequestBody IdListRequest req) {
        int affected = voucherService.approveByIds(req.getIds());
        return BulkResultResponse.builder().affected(affected).build();
    }

    // POST /api/vouchers/delete  { "ids":[1,2,3] }
    @PostMapping("/delete")
    public BulkResultResponse delete(@RequestBody IdListRequest req) {
        int affected = voucherService.deleteByIds(req.getIds());
        return BulkResultResponse.builder().affected(affected).build();
    }
}