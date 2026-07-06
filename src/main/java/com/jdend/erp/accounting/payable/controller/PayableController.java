package com.jdend.erp.accounting.payable.controller;

import com.jdend.erp.accounting.payable.dto.PayableBulkPayRequest;
import com.jdend.erp.accounting.payable.dto.PayableLineResponse;
import com.jdend.erp.accounting.payable.service.PayableService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payables")
@RequiredArgsConstructor
public class PayableController {

    private final PayableService service;

    @GetMapping
    public List<PayableLineResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String accountName) {
        return service.search(startDate, endDate, accountName);
    }

    @GetMapping("/account-names")
    public List<String> accountNames() {
        return service.accountNames();
    }

    @PostMapping("/pay")
    public ResponseEntity<?> pay(@RequestBody PayableBulkPayRequest req) {
        try {
            return ResponseEntity.ok(service.bulkPay(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
