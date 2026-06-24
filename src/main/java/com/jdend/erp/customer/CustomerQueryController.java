package com.jdend.erp.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")

public class CustomerQueryController {

  private final CustomerRepository repo;

  @GetMapping("/lookup")
  public Customer lookup(@RequestParam String customerNumber) {
    return repo.findByCustomerNumber(customerNumber)
        .orElseThrow(() -> new RuntimeException("고객 없음: " + customerNumber));
  }

  // ✅ keyword / kw 둘 다 지원 (프론트 실수해도 404 안 나게)
  @GetMapping("/search")
  public List<Customer> search(
      @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
      @RequestParam(value = "kw", required = false, defaultValue = "") String kw
  ) {
    String merged = (keyword != null && !keyword.isBlank()) ? keyword : kw;
    String k = (merged == null) ? "" : merged.trim();
    if (k.isBlank()) return repo.findTop200ByOrderByIdDesc();
    return repo.searchTop200(k);
  }
}