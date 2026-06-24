package com.jdend.erp.dashboard.controller;

import com.jdend.erp.dashboard.dto.*;
import com.jdend.erp.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")

public class DashboardController {

  private final DashboardService service;

  @GetMapping("/cash-daily")
  public DashboardCashResponse cashDaily(@RequestParam(required = false) LocalDate baseDate) {
    return service.cashDaily(baseDate);
  }

  @GetMapping("/contract-status")
  public DashboardContractStatusResponse contractStatus() {
    return service.contractStatus();
  }

  @GetMapping("/insurance-expiring")
  public List<DashboardInsuranceRow> insuranceExpiring(
      @RequestParam(defaultValue = "7") int days,
      @RequestParam(defaultValue = "5") int limit
  ) {
    return service.insuranceExpiring(days, limit);
  }

  @GetMapping("/maturity-soon")
  public List<DashboardMaturityRow> maturitySoon(
      @RequestParam(defaultValue = "30") int days,
      @RequestParam(defaultValue = "5") int limit
  ) {
    return service.maturitySoon(days, limit);
  }

  @GetMapping("/receivables-top")
  public List<DashboardReceivableRow> receivablesTop(@RequestParam(defaultValue = "5") int limit) {
    return service.receivablesTop(limit);
  }
}