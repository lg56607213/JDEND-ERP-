package com.jdend.erp.accounting.deposit.controller;

import com.jdend.erp.accounting.deposit.dto.DepositCreateRequest;
import com.jdend.erp.accounting.deposit.dto.DepositHistoryResponse;
import com.jdend.erp.accounting.deposit.dto.DepositListItemResponse;
import com.jdend.erp.accounting.deposit.service.DepositTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deposit-transactions")
@RequiredArgsConstructor
public class DepositTransactionController {

    private final DepositTransactionService service;

    /**
     * GET /api/deposit-transactions
     * 보증금/선수금 계약 목록
     * @param startDate 계약기간 시작 필터 (계약 종료일 >= startDate)
     * @param endDate   계약기간 종료 필터 (계약 시작일 <= endDate)
     * @param type      보증금 | 선수금 | 전체 (미입력=전체)
     * @param customerName 고객명 부분 검색
     */
    @GetMapping
    public ResponseEntity<List<DepositListItemResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "") String type,
            @RequestParam(required = false, defaultValue = "") String customerName) {
        return ResponseEntity.ok(service.list(startDate, endDate, type, customerName));
    }

    /**
     * GET /api/deposit-transactions/{contractId}/history?depositType=보증금
     * 특정 계약의 보증금/선수금 거래 이력
     */
    @GetMapping("/{contractId}/history")
    public ResponseEntity<List<DepositHistoryResponse>> history(
            @PathVariable Long contractId,
            @RequestParam(required = false, defaultValue = "보증금") String depositType) {
        return ResponseEntity.ok(service.history(contractId, depositType));
    }

    /**
     * POST /api/deposit-transactions/collect
     * 수납 등록 (보증금/선수금을 고객으로부터 받음)
     */
    @PostMapping("/collect")
    public ResponseEntity<Map<String, String>> collect(@RequestBody DepositCreateRequest req) {
        service.collect(req);
        return ResponseEntity.ok(Map.of("message", "수납이 등록되었습니다."));
    }

    /**
     * POST /api/deposit-transactions/refund
     * 지급(반환) 등록 (만기/해지 시 고객에게 보증금 반환)
     */
    @PostMapping("/refund")
    public ResponseEntity<Map<String, String>> refund(@RequestBody DepositCreateRequest req) {
        service.refund(req);
        return ResponseEntity.ok(Map.of("message", "지급(반환)이 등록되었습니다."));
    }
}
