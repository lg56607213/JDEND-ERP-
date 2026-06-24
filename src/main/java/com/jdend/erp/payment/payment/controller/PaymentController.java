package com.jdend.erp.payment.payment.controller;

import com.jdend.erp.payment.payment.dto.*;
import com.jdend.erp.payment.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")

public class PaymentController {

  private final PaymentService service;

  @GetMapping
  public Page<PaymentResponse> list(
      @RequestParam(value="kw", required=false, defaultValue="") String kw,
      @RequestParam(value="page", required=false, defaultValue="0") int page,
      @RequestParam(value="size", required=false, defaultValue="15") int size
  ) {
    return service.list(kw, page, size);
  }

  @GetMapping("/{id}")
  public PaymentResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public PaymentResponse create(@RequestBody PaymentUpsertRequest req) {
    return service.create(req);
  }

  @PutMapping("/{id}")
  public PaymentResponse update(@PathVariable Long id, @RequestBody PaymentUpsertRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}