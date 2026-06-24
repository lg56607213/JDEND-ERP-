package com.jdend.erp.payment.billing.controller;

import com.jdend.erp.payment.billing.dto.*;
import com.jdend.erp.payment.billing.entity.Billings;
import com.jdend.erp.payment.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/billings")

public class BillingController {

  private final BillingService billingService;

  @PostMapping("/create")
  public BillingCreateResponse create(@RequestBody BillingCreateRequest req) {
    return billingService.create(req);
  }

  @GetMapping
  public List<BillingListRowResponse> list(
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) String customerName,
      @RequestParam(required = false) String contractNumber
  ) {
    return billingService.search(startDate, endDate, customerName, contractNumber);
  }

  @GetMapping("/{billingNo}")
  public Billings detail(@PathVariable String billingNo) {
    return billingService.getOne(billingNo);
  }

  @PutMapping("/{billingNo}")
  public void update(@PathVariable String billingNo, @RequestBody BillingUpdateRequest req) {
    billingService.update(billingNo, req);
  }

  @GetMapping(value = "/{billingNo}/print", produces = MediaType.TEXT_HTML_VALUE)
  public String print(@PathVariable String billingNo) {
    Billings b = billingService.getOne(billingNo);
    return billingService.buildPrintHtml(b);
  }

  @PostMapping("/{billingNo}/send")
  public void send(@PathVariable String billingNo, @RequestBody BillingSendRequest req) {
    billingService.send(billingNo, req.getEmail());
  }
}