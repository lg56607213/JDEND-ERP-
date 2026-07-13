package com.jdend.erp.myinfo.controller;

import com.jdend.erp.myinfo.dto.*;
import com.jdend.erp.myinfo.service.MyInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/my-info")
public class MyInfoController {

    private final MyInfoService service;

    // ── 통장 ──────────────────────────────────────────────────

    @GetMapping("/bank-accounts")
    public List<BankAccountResponse> getBankAccounts(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return activeOnly ? service.listActiveBankAccounts() : service.listBankAccounts();
    }

    @PostMapping("/bank-accounts")
    public BankAccountResponse createBankAccount(@RequestBody BankAccountRequest req) {
        return service.createBankAccount(req);
    }

    @PutMapping("/bank-accounts/{id}")
    public BankAccountResponse updateBankAccount(@PathVariable Long id,
                                                  @RequestBody BankAccountRequest req) {
        return service.updateBankAccount(id, req);
    }

    @DeleteMapping("/bank-accounts/{id}")
    public ResponseEntity<Void> deleteBankAccount(@PathVariable Long id) {
        service.deleteBankAccount(id);
        return ResponseEntity.noContent().build();
    }

    // ── 법인카드 ───────────────────────────────────────────────

    @GetMapping("/corporate-cards")
    public List<CorporateCardResponse> getCorporateCards(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return activeOnly ? service.listActiveCorporateCards() : service.listCorporateCards();
    }

    @PostMapping("/corporate-cards")
    public CorporateCardResponse createCorporateCard(@RequestBody CorporateCardRequest req) {
        return service.createCorporateCard(req);
    }

    @PutMapping("/corporate-cards/{id}")
    public CorporateCardResponse updateCorporateCard(@PathVariable Long id,
                                                      @RequestBody CorporateCardRequest req) {
        return service.updateCorporateCard(id, req);
    }

    @DeleteMapping("/corporate-cards/{id}")
    public ResponseEntity<Void> deleteCorporateCard(@PathVariable Long id) {
        service.deleteCorporateCard(id);
        return ResponseEntity.noContent().build();
    }

    // ── 세금계산서 공급자 정보 ─────────────────────────────────

    @GetMapping("/supplier")
    public SupplierInfoResponse getSupplierInfo() {
        return service.getSupplierInfo();
    }

    @PutMapping("/supplier")
    public SupplierInfoResponse saveSupplierInfo(@RequestBody SupplierInfoRequest req) {
        return service.saveSupplierInfo(req);
    }
}
