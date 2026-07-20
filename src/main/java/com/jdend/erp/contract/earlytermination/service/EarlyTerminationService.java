package com.jdend.erp.contract.earlytermination.service;

import com.jdend.erp.accounting.settings.service.OtherAccountSettingsService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EarlyTerminationService {

  private final EarlyTerminationRepository earlyTerminationRepository;
  private final ContractRepository contractRepository;
  private final CustomerRepository customerRepository;
  private final VoucherService voucherService;
  private final OtherAccountSettingsService accountSettings;

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
   * 중도해지 > 반납 > 처리완료 시 전표 발생
   * 기타계정관리 earlyTermMapping 설정 기준으로 각 분개 항목의 차변/대변 계정을 결정한다.
   * 금액이 0인 항목과 계정이 미설정된 항목은 warn 로그 후 해당 분개를 건너뛴다.
   * 최종적으로 유효한 분개가 하나도 없으면 전표를 생성하지 않는다.
   */
  private void createReturnVoucher(EarlyTermination et) {
    long uncollectedRent   = safe(et.getUncollectedRent());
    long terminationAmount = safe(et.getTerminationAmount());
    long terminationFee    = safe(et.getTerminationFee());

    String unrDebit  = accountSettings.getEarlyTermUnrealizedRentDebit();
    String unrCredit = accountSettings.getEarlyTermUnrealizedRentCredit();
    String amtDebit  = accountSettings.getEarlyTermAmountDebit();
    String amtCredit = accountSettings.getEarlyTermAmountCredit();
    String feeDebit  = accountSettings.getEarlyTermFeeDebit();
    String feeCredit = accountSettings.getEarlyTermFeeCredit();

    List<VoucherCreateRequest.VoucherLineRequest> debitEntries  = new ArrayList<>();
    List<VoucherCreateRequest.VoucherLineRequest> creditEntries = new ArrayList<>();

    // 미회수렌트료 분개
    if (uncollectedRent > 0) {
      if (unrDebit == null || unrCredit == null) {
        log.warn("중도해지 미회수렌트료 분개 생략: 기타계정관리 > 중도해지 > 미회수렌트료 차변/대변을 설정해주세요. etId={}", et.getId());
      } else {
        debitEntries.add(VoucherCreateRequest.VoucherLineRequest.builder()
            .account(unrDebit).amount(uncollectedRent).description("미회수렌트료").build());
        creditEntries.add(VoucherCreateRequest.VoucherLineRequest.builder()
            .account(unrCredit).amount(uncollectedRent).description("미회수렌트료").build());
      }
    }

    // 중도상환금액 분개
    if (terminationAmount > 0) {
      if (amtDebit == null || amtCredit == null) {
        log.warn("중도해지 상환금액 분개 생략: 기타계정관리 > 중도해지 > 중도상환금액 차변/대변을 설정해주세요. etId={}", et.getId());
      } else {
        debitEntries.add(VoucherCreateRequest.VoucherLineRequest.builder()
            .account(amtDebit).amount(terminationAmount).description("중도상환금액").build());
        creditEntries.add(VoucherCreateRequest.VoucherLineRequest.builder()
            .account(amtCredit).amount(terminationAmount).description("중도상환금액").build());
      }
    }

    // 중도상환수수료 분개
    if (terminationFee > 0) {
      if (feeDebit == null || feeCredit == null) {
        log.warn("중도해지 수수료 분개 생략: 기타계정관리 > 중도해지 > 중도상환수수료 차변/대변을 설정해주세요. etId={}", et.getId());
      } else {
        debitEntries.add(VoucherCreateRequest.VoucherLineRequest.builder()
            .account(feeDebit).amount(terminationFee).description("중도상환수수료").build());
        creditEntries.add(VoucherCreateRequest.VoucherLineRequest.builder()
            .account(feeCredit).amount(terminationFee).description("중도상환수수료").build());
      }
    }

    if (debitEntries.isEmpty()) {
      log.warn("중도해지 전표 생략: 유효한 분개 항목이 없습니다. 기타계정관리에서 earlyTermMapping을 설정해주세요. etId={}", et.getId());
      return;
    }

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