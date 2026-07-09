package com.jdend.erp.contract.controller;

import com.jdend.erp.contract.dto.ContractSearchRowResponse;
import com.jdend.erp.contract.dto.ContractStatusRowResponse;
import com.jdend.erp.contract.dto.ContractSummaryResponse;
import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.customer.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contracts")

public class ContractQueryController {

  private final ContractRepository contractRepo;

  // ✅ 기존: 전체 계약 검색(모달 등)
  @GetMapping("/search")
  public List<ContractSearchRowResponse> search(
      @RequestParam(value = "kw", required = false, defaultValue = "") String kw
  ) {
    String keyword = (kw == null) ? "" : kw.trim();
    List<Contract> list = contractRepo.searchTop200(keyword);

    List<ContractSearchRowResponse> out = new ArrayList<>();
    for (Contract c : list) {
      String customerName = (c.getCustomer() != null) ? c.getCustomer().getCustomerName() : null;

      out.add(ContractSearchRowResponse.builder()
          .contractNumber(c.getContractNumber())
          .customerName(customerName)
          .vehicleNo(c.getVehicleNo())
          .contractType(c.getContractType())
          .startDate(c.getStartDate())
          .endDate(c.getEndDate())
          .monthlyRent(c.getMonthlyRent())
          .totalRent(c.getTotalRent())
          .build()
      );
    }
    return out;
  }

  // ✅✅ 추가: 수납등록 "가능" 계약만 검색 (돋보기용)
  // GET /api/contracts/payable-search?kw=
  @GetMapping("/payable-search")
  public List<ContractSearchRowResponse> payableSearch(
      @RequestParam(value = "kw", required = false, defaultValue = "") String kw
  ) {
    String keyword = (kw == null) ? "" : kw.trim();
    List<Contract> list = contractRepo.payableSearchTop200(keyword);

    List<ContractSearchRowResponse> out = new ArrayList<>();
    for (Contract c : list) {
      String customerName = (c.getCustomer() != null) ? c.getCustomer().getCustomerName() : null;

      out.add(ContractSearchRowResponse.builder()
          .contractNumber(c.getContractNumber())
          .customerName(customerName)
          .vehicleNo(c.getVehicleNo())
          .contractType(c.getContractType())
          .startDate(c.getStartDate())
          .endDate(c.getEndDate())
          .monthlyRent(c.getMonthlyRent())
          .totalRent(c.getTotalRent())
          .build()
      );
    }
    return out;
  }

  // ✅ 수납 화면 계약 요약
  @GetMapping("/{contractNumber}/summary")
  public ContractSummaryResponse summary(@PathVariable String contractNumber) {
    String cn = (contractNumber == null) ? "" : contractNumber.trim();

    Contract c = contractRepo.findWithCustomerByContractNumber(cn)
        .orElseThrow(() -> new RuntimeException("계약 없음: " + cn));

    Customer cu = c.getCustomer();

    String email = null;
    if (cu != null) {
      email = (cu.getBillEmail() != null && !cu.getBillEmail().isBlank())
          ? cu.getBillEmail()
          : cu.getManagerEmail();
    }

    String status = (c.getStatus() == null || c.getStatus().isBlank()) ? "진행중" : c.getStatus();

    return ContractSummaryResponse.builder()
        .contractNumber(c.getContractNumber())
        .vehicleNo(c.getVehicleNo())
        .customerName(cu != null ? cu.getCustomerName() : null)
        .registrationNumber(cu != null ? cu.getRegistrationNumber() : null)
        .email(email)
        .monthlyRent(c.getMonthlyRent())
        .contractStatus(status)
        .build();
  }

  private static final Set<String> TERMINATED_STATUSES = Set.of(
      "종료", "만기종료", "해지", "중도해지", "중도상환", "만기상환", "완료", "종결"
  );

  // GET /api/contracts/status?contractNumber=&customerName=&vehicleNo=&contractStatus=
  @GetMapping("/status")
  public List<ContractStatusRowResponse> status(
      @RequestParam(required = false, defaultValue = "") String contractNumber,
      @RequestParam(required = false, defaultValue = "") String customerName,
      @RequestParam(required = false, defaultValue = "") String vehicleNo,
      @RequestParam(required = false, defaultValue = "") String contractStatus
  ) {
    List<ContractStatusRowResponse> list = contractRepo.statusList(
        contractNumber == null ? "" : contractNumber.trim(),
        customerName == null ? "" : customerName.trim(),
        vehicleNo == null ? "" : vehicleNo.trim()
    );

    LocalDate today = LocalDate.now();
    for (ContractStatusRowResponse row : list) {
      String s = row.getStatus();
      String derived;
      if (s != null && TERMINATED_STATUSES.contains(s)) {
        derived = "종료";
      } else if (row.getContractEnd() != null && row.getContractEnd().isBefore(today)) {
        derived = "종료";
      } else if ("연체".equals(s)) {
        derived = "연체";
      } else {
        derived = "정상";
      }
      row.setContractStatus(derived);
    }

    String filter = (contractStatus == null) ? "" : contractStatus.trim();
    if (!filter.isEmpty()) {
      list = list.stream().filter(r -> filter.equals(r.getContractStatus())).toList();
    }

    return list;
  }
}