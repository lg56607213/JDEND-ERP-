package com.jdend.erp.payment.billing.controller;

import com.jdend.erp.payment.billing.dto.ContractSearchRow;
import com.jdend.erp.payment.billing.dto.CustomerSearchRow;
import com.jdend.erp.payment.billing.entity.Contracts;
import com.jdend.erp.payment.billing.entity.Customers;
import com.jdend.erp.payment.billing.repository.ContractsRepository;
import com.jdend.erp.payment.billing.repository.CustomersLookupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/billings")

public class BillingLookupController {

  private final CustomersLookupRepository customersRepo;
  private final ContractsRepository contractsRepo;

  // ✅ 거래처 돋보기: /api/billings/customers/search?kw=
  @GetMapping("/customers/search")
  public List<CustomerSearchRow> searchCustomers(
      @RequestParam(value = "kw", required = false, defaultValue = "") String kw
  ) {
    return customersRepo.search(kw).stream()
        .map(this::toCustomerRow)
        .toList();
  }

  private CustomerSearchRow toCustomerRow(Customers c) {
    String email = (c.getBillEmail() != null && !c.getBillEmail().isBlank())
        ? c.getBillEmail()
        : (c.getManagerEmail() == null ? "" : c.getManagerEmail());
    return new CustomerSearchRow(c.getCustomerNumber(), c.getCustomerName(), email);
  }

  // ✅ 계약 돋보기: /api/billings/contracts/search?kw=
  @GetMapping("/contracts/search")
  public List<ContractSearchRow> searchContracts(
      @RequestParam(value = "kw", required = false, defaultValue = "") String kw
  ) {
    List<Contracts> list = contractsRepo.search(kw);
    return list.stream()
        .map(c -> new ContractSearchRow(c.getContractNumber(), c.getCustomerNumber()))
        .toList();
  }
}