package com.jdend.erp.contract.maturity.service;

import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.contract.maturity.dto.*;
import com.jdend.erp.contract.maturity.entity.MaturityManagement;
import com.jdend.erp.contract.maturity.repository.MaturityManagementRepository;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.contract.service.ContractService;
import com.jdend.erp.customer.Customer;
import com.jdend.erp.customer.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MaturityManagementService {

  private final MaturityManagementRepository repo;
  private final ContractRepository contractRepository;
  private final CustomerRepository customerRepository;
  private final ContractService contractService;

  @Transactional(readOnly = true)
  public Page<MaturityRowDto> list(String status, int page, int size) {
    Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id"));

    Page<MaturityManagement> p;
    if (status == null || status.isBlank() || "all".equalsIgnoreCase(status) || "전체".equals(status)) {
      p = repo.findAll(pageable);
    } else {
      p = repo.findByStatus(status, pageable);
    }
    return p.map(this::toRow);
  }

  @Transactional(readOnly = true)
  public MaturityDetailResponse get(Long id) {
    MaturityManagement mm = repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("만기관리 데이터를 찾을 수 없습니다. id=" + id));
    return toDetail(mm);
  }

  public Long create(MaturityCreateRequest req) {
    Contract old = contractRepository.findByContractNumber(req.getOldContractNumber())
        .orElseThrow(() -> new IllegalArgumentException("기존 계약번호를 찾을 수 없습니다: " + req.getOldContractNumber()));

    String customerName = resolveCustomerName(old);

    // 신규 계약번호는 ContractService와 동일한 채번 규칙(동시성 보호 포함)을 그대로 재사용한다.
    String newContractNumber = contractService.generateNextContractNumber();

    MaturityManagement mm = MaturityManagement.builder()
        .oldContractId(old.getId())
        .oldContractNumber(old.getContractNumber())
        .customerName(customerName)
        .vehicleNo(old.getVehicleNo())
        .oldEndDate(old.getEndDate())
        .newContractNumber(newContractNumber)
        .newStartDate(req.getNewStartDate())
        .newEndDate(req.getNewEndDate())
        .newMonthlyRent(safe(req.getNewMonthlyRent()))
        .status(req.getStatus())
        .build();

    return repo.save(mm).getId();
  }

  public void update(Long id, MaturityUpdateRequest req) {
    MaturityManagement mm = repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("만기관리 데이터를 찾을 수 없습니다. id=" + id));

    mm.setNewStartDate(req.getNewStartDate());
    mm.setNewEndDate(req.getNewEndDate());
    mm.setNewMonthlyRent(safe(req.getNewMonthlyRent()));
    mm.setStatus(req.getStatus());
  }

  public void delete(Long id) {
    repo.deleteById(id);
  }

  private MaturityRowDto toRow(MaturityManagement m) {
    return MaturityRowDto.builder()
        .id(m.getId())
        .oldContractNumber(m.getOldContractNumber())
        .newContractNumber(m.getNewContractNumber())
        .customerName(m.getCustomerName())
        .vehicleNo(m.getVehicleNo())
        .oldEndDate(m.getOldEndDate())
        .newStartDate(m.getNewStartDate())
        .newEndDate(m.getNewEndDate())
        .newMonthlyRent(m.getNewMonthlyRent())
        .status(m.getStatus())
        .build();
  }

  private MaturityDetailResponse toDetail(MaturityManagement m) {
    return MaturityDetailResponse.builder()
        .id(m.getId())
        .oldContractId(m.getOldContractId())
        .oldContractNumber(m.getOldContractNumber())
        .customerName(m.getCustomerName())
        .vehicleNo(m.getVehicleNo())
        .oldEndDate(m.getOldEndDate())
        .newContractNumber(m.getNewContractNumber())
        .newStartDate(m.getNewStartDate())
        .newEndDate(m.getNewEndDate())
        .newMonthlyRent(m.getNewMonthlyRent())
        .status(m.getStatus())
        .build();
  }

  private String resolveCustomerName(Contract contract) {
    String customerNumber = contract.getCustomerNumber();
    if (customerNumber == null || customerNumber.isBlank()) return "-";
    Customer c = customerRepository.findByCustomerNumber(customerNumber).orElse(null);
    return (c != null) ? c.getCustomerName() : "-";
  }

  private long safe(Long v) {
    return (v == null) ? 0L : v;
  }

}