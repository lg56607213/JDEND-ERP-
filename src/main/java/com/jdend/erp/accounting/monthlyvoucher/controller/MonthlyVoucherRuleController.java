package com.jdend.erp.accounting.monthlyvoucher.controller;

import com.jdend.erp.accounting.monthlyvoucher.dto.MonthlyVoucherRuleCreateRequest;
import com.jdend.erp.accounting.monthlyvoucher.dto.MonthlyVoucherRuleCreateResponse;
import com.jdend.erp.accounting.monthlyvoucher.service.MonthlyVoucherRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounting/monthly-voucher-rules")

public class MonthlyVoucherRuleController {

  private final MonthlyVoucherRuleService service;

  @PostMapping
  public MonthlyVoucherRuleCreateResponse create(@RequestBody MonthlyVoucherRuleCreateRequest req) {
    return service.create(req);
  }
}