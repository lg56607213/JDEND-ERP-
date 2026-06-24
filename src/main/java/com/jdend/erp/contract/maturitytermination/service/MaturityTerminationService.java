package com.jdend.erp.contract.maturitytermination.service;

import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.contract.maturitytermination.dto.*;
import com.jdend.erp.contract.maturitytermination.entity.MaturityTermination;
import com.jdend.erp.contract.maturitytermination.repository.MaturityTerminationRepository;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.customer.Customer;
import com.jdend.erp.customer.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MaturityTerminationService {

  private final MaturityTerminationRepository repo;
  private final ContractRepository contractRepository;
  private final CustomerRepository customerRepository;

  @Transactional(readOnly = true)
  public Page<MaturityTerminationRowDto> list(String status, int page, int size) {
    Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id"));

    Page<MaturityTermination> p;
    if (status == null || status.isBlank() || "all".equalsIgnoreCase(status) || "전체".equals(status)) {
      p = repo.findAll(pageable);
    } else {
      p = repo.findByStatus(status, pageable);
    }

    return p.map(this::toRow);
  }

  @Transactional(readOnly = true)
  public MaturityTerminationDetailResponse get(Long id) {
    MaturityTermination t = repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("만기종료 데이터를 찾을 수 없습니다. id=" + id));
    return toDetail(t);
  }

  public Long create(MaturityTerminationCreateRequest req) {
    Contract c = contractRepository.findByContractNumber(req.getContractNumber())
        .orElseThrow(() -> new IllegalArgumentException("계약번호를 찾을 수 없습니다: " + req.getContractNumber()));

    String customerName = resolveCustomerName(c);

    MaturityTermination t = MaturityTermination.builder()
        .contractId(c.getId())
        .contractNumber(c.getContractNumber())
        .customerName(customerName)
        .vehicleNo(c.getVehicleNo())
        .contractType(c.getContractType())
        .startDate(c.getStartDate())
        .endDate(c.getEndDate())
        .monthlyRent(safe(c.getMonthlyRent()))
        .terminationMethod(req.getTerminationMethod())
        .terminationDate(req.getTerminationDate())
        .unpaidAmount(safe(req.getUnpaidAmount()))
        .status(req.getStatus())
        .build();

    return repo.save(t).getId();
  }

  public void update(Long id, MaturityTerminationUpdateRequest req) {
    MaturityTermination t = repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("만기종료 데이터를 찾을 수 없습니다. id=" + id));

    t.setTerminationMethod(req.getTerminationMethod());
    t.setTerminationDate(req.getTerminationDate());
    t.setUnpaidAmount(safe(req.getUnpaidAmount()));
    t.setStatus(req.getStatus());
  }

  public void delete(Long id) {
    repo.deleteById(id);
  }

  private MaturityTerminationRowDto toRow(MaturityTermination t) {
    return MaturityTerminationRowDto.builder()
        .id(t.getId())
        .contractNumber(t.getContractNumber())
        .customerName(t.getCustomerName())
        .vehicleNo(t.getVehicleNo())
        .terminationMethod(t.getTerminationMethod())
        .terminationDate(t.getTerminationDate())
        .unpaidAmount(t.getUnpaidAmount())
        .status(t.getStatus())
        .build();
  }

  private MaturityTerminationDetailResponse toDetail(MaturityTermination t) {
    return MaturityTerminationDetailResponse.builder()
        .id(t.getId())
        .contractId(t.getContractId())
        .contractNumber(t.getContractNumber())
        .customerName(t.getCustomerName())
        .vehicleNo(t.getVehicleNo())
        .contractType(t.getContractType())
        .startDate(t.getStartDate())
        .endDate(t.getEndDate())
        .monthlyRent(t.getMonthlyRent())
        .terminationMethod(t.getTerminationMethod())
        .terminationDate(t.getTerminationDate())
        .unpaidAmount(t.getUnpaidAmount())
        .status(t.getStatus())
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