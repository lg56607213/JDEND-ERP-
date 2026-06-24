package com.jdend.erp.accounting.cash.controller;

import com.jdend.erp.accounting.cash.dto.*;
import com.jdend.erp.accounting.cash.service.DailyCashService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cash")

public class DailyCashController {

  private final DailyCashService service;

  // GET /api/cash/daily-cash-flow?month=2025-11
  @GetMapping("/daily-cash-flow")
  public DailyCashFlowMonthResponse month(@RequestParam String month) {
    return service.month(month);
  }

  // GET /api/cash/daily-fund-report?date=2025-11-03
  @GetMapping("/daily-fund-report")
  public DailyFundReportResponse daily(@RequestParam LocalDate date) {
    return service.daily(date);
  }
}