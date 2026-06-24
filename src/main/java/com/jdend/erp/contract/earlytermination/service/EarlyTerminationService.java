package com.jdend.erp.contract.earlytermination.service;

import com.jdend.erp.accounting.voucher.dto.VoucherCreateRequest;
import com.jdend.erp.accounting.voucher.service.VoucherService;
import com.jdend.erp.contract.earlytermination.dto.*;
import com.jdend.erp.contract.earlytermination.entity.EarlyTermination;
import com.jdend.erp.contract.earlytermination.repository.EarlyTerminationRepository;
import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.customer.Customer;
import com.jdend.erp.customer.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EarlyTerminationService {

  private final EarlyTerminationRepository earlyTerminationRepository;
  private final ContractRepository contractRepository;
  private final CustomerRepository customerRepository;
  private final VoucherService voucherService;

  @Transactional(readOnly = true)
  public Page<EarlyTerminationRowDto> list(String status, int page, int size) {
    Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id"));

    Page<EarlyTermination> result;
    if (status == null || status.isBlank() || "all".equalsIgnoreCase(status) || "전체".equals(status)) {
      result = earlyTerminationRepository.findAll(pageable);
    } else {
      result = earlyTerminationRepository.findByStatus(status, pageable);
    }

    return result.map(this::toRowDto);
  }

  @Transactional(readOnly = true)
  public EarlyTerminationDetailResponse get(Long id) {
    EarlyTermination et = earlyTerminationRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("중도상환 데이터를 찾을 수 없습니다. id=" + id));
    return toDetailDto(et);
  }

  public Long create(EarlyTerminationCreateRequest req) {
    if (req.getTerminationFee() == null) req.setTerminationFee(0L);
    if (req.getUncollectedRent() == null) req.setUncollectedRent(0L);

    Contract contract = contractRepository.findByContractNumber(req.getContractNumber())
        .orElseThrow(() -> new IllegalArgumentException("계약번호를 찾을 수 없습니다: " + req.getContractNumber()));

    String customerName = resolveCustomerName(contract);
    LocalDate terminationDate = (req.getTerminationDate() != null) ? req.getTerminationDate() : LocalDate.now();

    long terminationAmount = safe(req.getTerminationAmount());
    long uncollectedRent = safe(req.getUncollectedRent());   // ✅ 직접 입력값 사용
    long terminationFee = safe(req.getTerminationFee());
    long totalAmount = terminationAmount + uncollectedRent + terminationFee;

    EarlyTermination et = EarlyTermination.builder()
        .contractId(contract.getId())
        .contractNumber(contract.getContractNumber())
        .customerName(customerName)
        .vehicleNo(contract.getVehicleNo())
        .contractType(contract.getContractType())
        .startDate(contract.getStartDate())
        .endDate(contract.getEndDate())
        .monthlyRent(contract.getMonthlyRent())
        .totalRent(contract.getTotalRent())
        .terminationMethod(req.getTerminationMethod())
        .terminationDate(terminationDate)
        .status(req.getStatus())
        .terminationAmount(terminationAmount)
        .uncollectedRent(uncollectedRent)   // ✅ 직접 입력값 저장
        .terminationFee(terminationFee)
        .totalAmount(totalAmount)
        .build();

    EarlyTermination saved = earlyTerminationRepository.save(et);

    // ✅ 중도상환 > 반납 > 처리완료 인 경우 전표 발생
    if (isReturnCompleted(saved)) {
      createReturnVoucher(saved);
    }

    return saved.getId();
  }

  public void update(Long id, EarlyTerminationUpdateRequest req) {
    if (req.getTerminationFee() == null) req.setTerminationFee(0L);
    if (req.getUncollectedRent() == null) req.setUncollectedRent(0L);

    EarlyTermination et = earlyTerminationRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("중도상환 데이터를 찾을 수 없습니다. id=" + id));

    LocalDate terminationDate = (req.getTerminationDate() != null) ? req.getTerminationDate() : et.getTerminationDate();

    long terminationAmount = safe(req.getTerminationAmount());
    long uncollectedRent = safe(req.getUncollectedRent());   // ✅ 직접 입력값 사용
    long terminationFee = safe(req.getTerminationFee());
    long totalAmount = terminationAmount + uncollectedRent + terminationFee;

    et.setTerminationMethod(req.getTerminationMethod());
    et.setTerminationDate(terminationDate);
    et.setStatus(req.getStatus());

    et.setTerminationAmount(terminationAmount);
    et.setUncollectedRent(uncollectedRent);  // ✅ 직접 입력값 저장
    et.setTerminationFee(terminationFee);
    et.setTotalAmount(totalAmount);
  }

  public void delete(Long id) {
    earlyTerminationRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public ContractLookupResponse lookupContract(String contractNumber) {
    Contract contract = contractRepository.findByContractNumber(contractNumber)
        .orElseThrow(() -> new IllegalArgumentException("계약번호를 찾을 수 없습니다: " + contractNumber));

    return ContractLookupResponse.builder()
        .contractId(contract.getId())
        .contractNumber(contract.getContractNumber())
        .customerName(resolveCustomerName(contract))
        .vehicleNo(contract.getVehicleNo())
        .contractType(contract.getContractType())
        .startDate(contract.getStartDate())
        .endDate(contract.getEndDate())
        .monthlyRent(contract.getMonthlyRent())
        .totalRent(contract.getTotalRent())
        .build();
  }

  @Transactional(readOnly = true)
  public List<ContractSearchRowDto> searchContracts(String kw) {
    String q = (kw == null) ? "" : kw.trim();
    List<Contract> list = contractRepository.searchTop200(q);

    return list.stream().map(c -> ContractSearchRowDto.builder()
        .contractId(c.getId())
        .contractNumber(c.getContractNumber())
        .customerName(resolveCustomerName(c))
        .vehicleNo(c.getVehicleNo())
        .contractType(c.getContractType())
        .startDate(c.getStartDate())
        .endDate(c.getEndDate())
        .monthlyRent(c.getMonthlyRent())
        .totalRent(c.getTotalRent())
        .build()
    ).toList();
  }

  private EarlyTerminationRowDto toRowDto(EarlyTermination e) {
    return EarlyTerminationRowDto.builder()
        .id(e.getId())
        .contractNumber(e.getContractNumber())
        .customerName(e.getCustomerName())
        .vehicleNo(e.getVehicleNo())
        .terminationMethod(e.getTerminationMethod())
        .terminationDate(e.getTerminationDate())
        .terminationAmount(e.getTerminationAmount())
        .uncollectedRent(e.getUncollectedRent())
        .totalAmount(e.getTotalAmount())
        .status(e.getStatus())
        .build();
  }

  private EarlyTerminationDetailResponse toDetailDto(EarlyTermination e) {
    return EarlyTerminationDetailResponse.builder()
        .id(e.getId())
        .contractId(e.getContractId())
        .contractNumber(e.getContractNumber())
        .customerName(e.getCustomerName())
        .vehicleNo(e.getVehicleNo())
        .contractType(e.getContractType())
        .startDate(e.getStartDate())
        .endDate(e.getEndDate())
        .monthlyRent(e.getMonthlyRent())
        .totalRent(e.getTotalRent())
        .terminationMethod(e.getTerminationMethod())
        .terminationDate(e.getTerminationDate())
        .status(e.getStatus())
        .terminationAmount(e.getTerminationAmount())
        .uncollectedRent(e.getUncollectedRent())
        .terminationFee(e.getTerminationFee())
        .totalAmount(e.getTotalAmount())
        .build();
  }

  private String resolveCustomerName(Contract contract) {
    String customerNumber = contract.getCustomerNumber();
    if (customerNumber == null || customerNumber.isBlank()) return "-";

    Customer c = customerRepository.findByCustomerNumber(customerNumber).orElse(null);
    return (c != null) ? c.getCustomerName() : "-";
  }

  private boolean isReturnCompleted(EarlyTermination et) {
    return "반납".equals(et.getTerminationMethod()) && "처리완료".equals(et.getStatus());
  }

  /**
   * ✅ 중도상환 > 반납 > 전표발생
   * - 차변: 미수금 (합계금액)
   * - 대변: 미회수렌트료(고정항목, 직접입력금액) / 중도상환금액 / 중도상환수수료
   *
   * 현재 전표승인 화면은 첫 CREDIT 라인만 보여주므로
   * "미회수렌트료"를 가장 먼저 넣어서 화면에서도 고정 항목처럼 보이게 처리.
   */
  private void createReturnVoucher(EarlyTermination et) {
    long terminationAmount = safe(et.getTerminationAmount());
    long uncollectedRent = safe(et.getUncollectedRent());
    long terminationFee = safe(et.getTerminationFee());
    long totalAmount = safe(et.getTotalAmount());

    List<VoucherCreateRequest.VoucherLineRequest> debitEntries = List.of(
        VoucherCreateRequest.VoucherLineRequest.builder()
            .account("미수금")
            .amount(totalAmount)
            .description("중도상환 반납")
            .build()
    );

    List<VoucherCreateRequest.VoucherLineRequest> creditEntries = new ArrayList<>();

    // ✅ 미회수렌트료는 고정 항목명 / 금액은 직접 입력값
    creditEntries.add(
        VoucherCreateRequest.VoucherLineRequest.builder()
            .account("미회수렌트료")
            .amount(uncollectedRent)
            .description("미회수렌트료")
            .build()
    );

    creditEntries.add(
        VoucherCreateRequest.VoucherLineRequest.builder()
            .account("중도상환금액")
            .amount(terminationAmount)
            .description("중도상환금액")
            .build()
    );

    creditEntries.add(
        VoucherCreateRequest.VoucherLineRequest.builder()
            .account("중도상환수수료")
            .amount(terminationFee)
            .description("중도상환수수료")
            .build()
    );

    voucherService.create(
        VoucherCreateRequest.builder()
            .voucherDate(et.getTerminationDate() != null ? et.getTerminationDate() : LocalDate.now())
            .contractNumber(et.getContractNumber())
            .vehicleNo(et.getVehicleNo())
            .memo("중도상환 반납")
            .debitEntries(debitEntries)
            .creditEntries(creditEntries)
            .build()
    );
  }

  private long safe(Long v) {
    return (v == null) ? 0L : v;
  }
}