package com.jdend.erp.management.financial.controller;

import com.jdend.erp.management.financial.dto.FinancialStatementAccountRequest;
import com.jdend.erp.management.financial.dto.FinancialStatementAccountResponse;
import com.jdend.erp.management.financial.dto.FinancialStatementVoucherRowResponse;
import com.jdend.erp.management.financial.service.FinancialStatementAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/financial-statement-accounts")

public class FinancialStatementAccountController {

  private final FinancialStatementAccountService service;

  @GetMapping
  public List<FinancialStatementAccountResponse> list(@RequestParam String statementType) {
    return service.list(statementType);
  }

  @PostMapping
  public FinancialStatementAccountResponse create(@RequestBody FinancialStatementAccountRequest req) {
    return service.create(req);
  }

  @PutMapping("/{id}")
  public FinancialStatementAccountResponse update(
      @PathVariable Long id,
      @RequestBody FinancialStatementAccountRequest req
  ) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }

  @GetMapping("/{id}/voucher-rows")
  public List<FinancialStatementVoucherRowResponse> getVoucherRows(
      @PathVariable Long id,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate
  ) {
    return service.getVoucherRows(id, startDate, endDate);
  }
}