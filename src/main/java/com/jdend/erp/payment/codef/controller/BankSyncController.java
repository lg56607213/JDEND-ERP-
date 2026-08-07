package com.jdend.erp.payment.codef.controller;

import com.jdend.erp.payment.codef.entity.CodefConnection;
import com.jdend.erp.payment.codef.repository.CodefConnectionRepository;
import com.jdend.erp.payment.codef.service.CodefService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bank-sync")
@RequiredArgsConstructor
public class BankSyncController {

    private final CodefService codefService;
    private final CodefConnectionRepository connectionRepository;

    /**
     * 등록된 계좌 목록 조회
     */
    @GetMapping("/connections")
    public ResponseEntity<List<CodefConnection>> listConnections() {
        return ResponseEntity.ok(connectionRepository.findByActiveTrue());
    }

    /**
     * 공동인증서 등록 (Connected ID 생성)
     * multipart: certFile, certPassword, organization, bankName, accountNo, clientType
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestParam("certFile")     MultipartFile certFile,
            @RequestParam("keyFile")      MultipartFile keyFile,
            @RequestParam("certPassword") String certPassword,
            @RequestParam("organization") String organization,
            @RequestParam("bankName")     String bankName,
            @RequestParam(value = "accountNo",  required = false) String accountNo,
            @RequestParam(value = "clientType", defaultValue = "B") String clientType) {
        try {
            CodefConnection conn = codefService.registerCertificate(
                    certFile.getBytes(), keyFile.getBytes(), certPassword, organization, bankName, accountNo, clientType);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "id", conn.getId(),
                    "bankName", conn.getBankName(),
                    "connectedId", conn.getConnectedId()
            ));
        } catch (Exception e) {
            log.error("[BankSync] 인증서 등록 실패", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 수동 최신화 버튼 — 해당 계좌의 거래내역을 CODEF에서 가져와 저장
     * @param startDate yyyyMMdd (없으면 30일 전)
     * @param endDate   yyyyMMdd (없으면 오늘)
     */
    @PostMapping("/sync/{id}")
    public ResponseEntity<Map<String, Object>> sync(
            @PathVariable Long id,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Map<String, Object> result = codefService.syncTransactions(id, startDate, endDate);
            return ResponseEntity.ok(Map.of("success", true, "result", result));
        } catch (Exception e) {
            log.error("[BankSync] 거래내역 동기화 실패: id={}", id, e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 연동 계좌 비활성화 (삭제 대신)
     */
    @DeleteMapping("/connections/{id}")
    public ResponseEntity<Map<String, Object>> deactivate(@PathVariable Long id) {
        connectionRepository.findById(id).ifPresent(c -> {
            c.setActive(false);
            connectionRepository.save(c);
        });
        return ResponseEntity.ok(Map.of("success", true));
    }
}
