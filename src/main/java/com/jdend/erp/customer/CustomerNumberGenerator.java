package com.jdend.erp.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 고객번호 자동 채번(C001, C002 ...). CustomerController/CustomerBulkUploadService가 공유한다. */
@Component
@RequiredArgsConstructor
public class CustomerNumberGenerator {

  private final CustomerRepository repo;

  // synchronized: 동시에 두 고객이 등록되면 둘 다 같은 max값을 읽어 같은 번호를 만들 수 있다.
  public synchronized String next() {
    String max = repo.findMaxCustomerNumber(); // 예: "C012" 또는 null
    int next = 1;

    if (max != null && max.startsWith("C")) {
      String num = max.substring(1).replaceAll("[^0-9]", "");
      if (!num.isBlank()) {
        try {
          next = Integer.parseInt(num) + 1;
        } catch (Exception ignored) {}
      }
    }

    return String.format("C%03d", next);
  }
}
