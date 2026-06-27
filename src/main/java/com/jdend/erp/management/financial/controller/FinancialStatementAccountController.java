package com.jdend.erp.management.financial.controller;

import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.management.financial.dto.FinancialStatementAccountRequest;
import com.jdend.erp.management.financial.dto.FinancialStatementAccountResponse;
import com.jdend.erp.management.financial.dto.FinancialStatementAccountTreeResponse;
import com.jdend.erp.management.financial.dto.FinancialStatementVoucherRowResponse;
import com.jdend.erp.management.financial.service.FinancialStatementAccountService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/financial-statement-accounts")

public class FinancialStatementAccountController {

  private final FinancialStatementAccountService service;
  private final PermissionService permissionService;

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
      @RequestBody FinancialStatementAccountRequest req,
      HttpSession session
  ) {
    permissionService.requireManager(session);
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id, HttpSession session) {
    permissionService.requireManager(session);
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

  @GetMapping("/tree")
  public List<FinancialStatementAccountTreeResponse> tree(@RequestParam String category) {
    return service.tree(category);
  }

  @GetMapping("/leaves")
  public List<FinancialStatementAccountResponse> leaves() {
    return service.leavesForVoucher();
  }

  @PostMapping("/nodes")
  public FinancialStatementAccountResponse createNode(@RequestBody FinancialStatementAccountRequest req) {
    return service.createNode(req);
  }
}