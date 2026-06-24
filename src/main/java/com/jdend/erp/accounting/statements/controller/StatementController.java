package com.jdend.erp.accounting.statements.controller;

import com.jdend.erp.accounting.statements.dto.*;
import com.jdend.erp.accounting.statements.service.StatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/statements")

public class StatementController {

  private final StatementService service;

  // GET /api/statements/income?startDate=2026-01-01&endDate=2026-03-03&status=승인
  @GetMapping("/income")
  public IncomeStatementResponse income(
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate,
      @RequestParam(required = false, defaultValue = "승인") String status
  ) {
    return service.income(startDate, endDate, status);
  }

  // GET /api/statements/balance?referenceDate=2026-03-03&status=승인
  @GetMapping("/balance")
  public BalanceSheetResponse balance(
      @RequestParam LocalDate referenceDate,
      @RequestParam(required = false, defaultValue = "승인") String status
  ) {
    return service.balance(referenceDate, status);
  }

  // GET /api/statements/balance/details?groupKey=cash&referenceDate=2026-04-20&status=승인
  @GetMapping("/balance/details")
  public List<BalanceDetailRowResponse> balanceDetails(
      @RequestParam String groupKey,
      @RequestParam LocalDate referenceDate,
      @RequestParam(required = false, defaultValue = "승인") String status
  ) {
    return service.balanceDetails(groupKey, referenceDate, status);
  }
}