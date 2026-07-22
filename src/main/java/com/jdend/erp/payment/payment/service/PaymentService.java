package com.jdend.erp.payment.payment.service;

import com.jdend.erp.accounting.settings.service.OtherAccountSettingsService;
import com.jdend.erp.accounting.voucher.entity.Voucher;
import com.jdend.erp.accounting.voucher.entity.VoucherLine;
import com.jdend.erp.accounting.voucher.repository.VoucherRepository;
import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.customer.Customer;
import com.jdend.erp.payment.payment.dto.PaymentResponse;
import com.jdend.erp.payment.payment.dto.PaymentUpsertRequest;
import com.jdend.erp.payment.payment.entity.Payment;
import com.jdend.erp.payment.payment.repository.PaymentRepository;
import com.jdend.erp.payment.receivable.entity.Receivable;
import com.jdend.erp.payment.receivable.repository.ReceivableRepository;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepo;
  private final ContractRepository contractRepo;
  private final VoucherRepository voucherRepository;
  private final OtherAccountSettingsService accountSettings;
  private final VehicleOrderRepository vehicleOrderRepo;
  private final ReceivableRepository receivableRepo;  // BUG-10

  @Transactional(readOnly = true)
  public Page<PaymentResponse> list(String kw, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    return paymentRepo.search(kw, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public PaymentResponse get(Long id) {
    Payment p = paymentRepo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("수납 ID를 찾을 수 없습니다: " + id));
    return toResponse(p);
  }

  @Transactional
  public PaymentResponse create(PaymentUpsertRequest req) {
    validate(req);

    Contract c = contractRepo.findWithCustomerByContractNumber(req.getContractNumber().trim())
        .orElseThrow(() -> new IllegalArgumentException("계약번호를 찾을 수 없습니다: " + req.getContractNumber()));

    Customer cu = c.getCustomer();

    Payment saved = paymentRepo.save(Payment.builder()
        .contractId(c.getId())
        .contractNumber(c.getContractNumber())
        .customerId(cu != null ? cu.getId() : null)
        .customerNumber(c.getCustomerNumber())
        .customerName(cu != null ? cu.getCustomerName() : null)
        .vehicleNo(c.getVehicleNo())
        .paymentDate(req.getPaymentDate())
        .paymentAmount(req.getPaymentAmount())
        .paymentMethod(req.getPaymentMethod())
        .companyAccount(req.getCompanyAccount())
        .memo(req.getMemo())
        .build());

    boolean shouldCreateVoucher = req.getCreateVoucher() == null || req.getCreateVoucher();
    if (shouldCreateVoucher) {
      // BUG-03: createVoucherIfNeeded가 생성된 전표 ID를 반환하고 Payment에 저장
      Long voucherId = createVoucherIfNeeded(saved);
      if (voucherId != null) {
        saved.setVoucherId(voucherId);
        paymentRepo.save(saved);
      }
    }

    // BUG-10: 수납 등록 후 동일 계약의 미납 미수금 상태 업데이트
    updateReceivableStatus(saved.getContractNumber(), saved.getPaymentAmount());

    return toResponse(saved);
  }

  @Transactional
  public PaymentResponse update(Long id, PaymentUpsertRequest req) {
    validate(req);

    Payment p = paymentRepo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("수납 ID를 찾을 수 없습니다: " + id));

    // BUG-03: 수정 전 미수금 상태 복구
    restoreReceivableStatus(p.getContractNumber(), p.getPaymentAmount());

    // BUG-03: 기존 연결 전표 삭제
    if (p.getVoucherId() != null) {
      voucherRepository.findById(p.getVoucherId()).ifPresent(v -> {
        voucherRepository.delete(v);
        log.info("수납 수정: 기존 전표 삭제 voucherId={}", p.getVoucherId());
      });
      p.setVoucherId(null);
    }

    String newCn = req.getContractNumber() == null ? "" : req.getContractNumber().trim();
    if (!newCn.isEmpty() && !newCn.equals(p.getContractNumber())) {
      Contract c = contractRepo.findWithCustomerByContractNumber(newCn)
          .orElseThrow(() -> new IllegalArgumentException("계약번호를 찾을 수 없습니다: " + newCn));

      Customer cu = c.getCustomer();
      p.setContractId(c.getId());
      p.setContractNumber(c.getContractNumber());
      p.setCustomerId(cu != null ? cu.getId() : null);
      p.setCustomerNumber(c.getCustomerNumber());
      p.setCustomerName(cu != null ? cu.getCustomerName() : null);
      p.setVehicleNo(c.getVehicleNo());
    }

    p.setPaymentDate(req.getPaymentDate());
    p.setPaymentAmount(req.getPaymentAmount());
    p.setPaymentMethod(req.getPaymentMethod());
    p.setCompanyAccount(req.getCompanyAccount());
    p.setMemo(req.getMemo());

    // BUG-03: 수정 후 전표 재생성
    boolean shouldCreateVoucher = req.getCreateVoucher() == null || req.getCreateVoucher();
    if (shouldCreateVoucher) {
      Long newVoucherId = createVoucherIfNeeded(p);
      if (newVoucherId != null) {
        p.setVoucherId(newVoucherId);
      }
    }

    paymentRepo.save(p);

    // BUG-03: 수정 후 새 금액으로 미수금 상태 재적용
    updateReceivableStatus(p.getContractNumber(), req.getPaymentAmount());

    return toResponse(p);
  }

  @Transactional
  public void delete(Long id) {
    Payment p = paymentRepo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("수납 ID를 찾을 수 없습니다: " + id));

    // BUG-03: 연결 전표 먼저 삭제
    if (p.getVoucherId() != null) {
      voucherRepository.findById(p.getVoucherId()).ifPresent(v -> {
        voucherRepository.delete(v);
        log.info("수납 삭제: 연결 전표 삭제 voucherId={}", p.getVoucherId());
      });
    }

    // BUG-03: 삭제 시 미수금 상태 복구
    restoreReceivableStatus(p.getContractNumber(), p.getPaymentAmount());

    paymentRepo.deleteById(id);
  }

  /**
   * BUG-03: 전표 생성 후 생성된 전표 ID를 반환한다.
   * @return 생성된 Voucher ID, 생성 조건 미충족 시 null
   */
  private Long createVoucherIfNeeded(Payment payment) {
    if (payment == null) return null;
    if (payment.getPaymentAmount() == null || payment.getPaymentAmount() <= 0) return null;

    // 현금주의: 수납은 현금 실수령이므로 수단과 무관하게 수익 전표를 생성한다.
    // 기타계정관리에서 설정한 계정명 사용. 미설정이면 전표를 생성하지 않는다.
    String debitAccount  = accountSettings.getPaymentDebitAccount();
    String creditAccount = accountSettings.getPaymentCreditAccount();
    if (debitAccount == null || creditAccount == null) {
      log.warn("수납 전표 생략: 기타계정관리 > 수납 전표의 차변/대변을 설정해주세요. paymentId={}", payment.getId());
      return null;
    }

    LocalDate voucherDate = payment.getPaymentDate() != null ? payment.getPaymentDate() : LocalDate.now();
    String voucherNo = nextVoucherNo(voucherDate);

    String memo = buildPaymentVoucherMemo(payment);

    String vehicleMgmtNo = null;
    String pVehicleNo = blankToNull(payment.getVehicleNo());
    if (pVehicleNo != null) {
      vehicleMgmtNo = vehicleOrderRepo.findByVehicleNoNormalized(pVehicleNo)
          .map(vo -> vo.getVehicleMgmtNo())
          .orElse(null);
    }

    Voucher voucher = Voucher.builder()
        .voucherNo(voucherNo)
        .voucherDate(voucherDate)
        .contractNumber(blankToNull(payment.getContractNumber()))
        .vehicleNo(blankToNull(payment.getVehicleNo()))
        .vehicleMgmtNo(vehicleMgmtNo)
        .totalAmount(payment.getPaymentAmount())
        .status("대기")
        .memo(memo)
        .build();

    voucher.addLine(VoucherLine.builder()
        .lineType("DEBIT")
        .accountName(debitAccount)
        .amount(payment.getPaymentAmount())
        .description("수납등록 입금")
        .sortOrder(1)
        .build());

    voucher.addLine(VoucherLine.builder()
        .lineType("CREDIT")
        .accountName(creditAccount)
        .amount(payment.getPaymentAmount())
        .description("수납등록 입금")
        .sortOrder(2)
        .build());

    Voucher saved = voucherRepository.save(voucher);
    return saved.getId();
  }

  /**
   * BUG-10: 수납 등록 시 동일 계약번호의 미납 미수금 상태를 업데이트한다.
   * 수납액 >= 미수금액이면 '완납'으로 처리한다.
   */
  private void updateReceivableStatus(String contractNumber, Long paymentAmount) {
    if (contractNumber == null || contractNumber.isBlank()) return;
    if (paymentAmount == null || paymentAmount <= 0) return;

    List<Receivable> unpaid = receivableRepo.findByContractNumberAndStatus(contractNumber, "미납");
    if (unpaid.isEmpty()) return;

    long remaining = paymentAmount;
    for (Receivable r : unpaid) {
      long amt = r.getReceivableAmount() == null ? 0L : r.getReceivableAmount();
      if (remaining >= amt) {
        r.setStatus("완납");
        receivableRepo.save(r);
        remaining -= amt;
      } else {
        break;  // 잔여 수납액이 미수금보다 적으면 중단
      }
    }
  }

  /** BUG-03: 수납 삭제/수정 시 완납으로 처리된 미수금을 역순으로 미납으로 되돌린다. */
  private void restoreReceivableStatus(String contractNumber, Long paymentAmount) {
    if (contractNumber == null || contractNumber.isBlank()) return;
    if (paymentAmount == null || paymentAmount <= 0) return;

    List<Receivable> paid = receivableRepo.findByContractNumberAndStatusOrderByIdDesc(contractNumber, "완납");
    if (paid.isEmpty()) return;

    long toReverse = paymentAmount;
    for (Receivable r : paid) {
      long amt = r.getReceivableAmount() == null ? 0L : r.getReceivableAmount();
      if (toReverse >= amt) {
        r.setStatus("미납");
        receivableRepo.save(r);
        toReverse -= amt;
      } else {
        break;
      }
    }
  }

  private String nextVoucherNo(LocalDate date) {
    String ymd = date.toString().replace("-", "");
    Long maxSeq = voucherRepository.findMaxSequenceForDatePrefix(ymd);
    long next = (maxSeq == null ? 0L : maxSeq) + 1;
    String candidate = ymd + String.format("%05d", next);
    while (voucherRepository.existsByVoucherNo(candidate)) {
      next++;
      candidate = ymd + String.format("%05d", next);
    }
    return candidate;
  }

  private String buildPaymentVoucherMemo(Payment payment) {
    String contractNumber = blankToNull(payment.getContractNumber());
    String customerName = blankToNull(payment.getCustomerName());
    String companyAccount = blankToNull(payment.getCompanyAccount());

    StringBuilder sb = new StringBuilder("수납등록");
    if (contractNumber != null) sb.append(" / 계약번호: ").append(contractNumber);
    if (customerName != null) sb.append(" / 고객명: ").append(customerName);
    if (companyAccount != null) sb.append(" / 당사계좌: ").append(companyAccount);
    return sb.toString();
  }

  private void validate(PaymentUpsertRequest req) {
    if (req.getContractNumber() == null || req.getContractNumber().trim().isEmpty()) {
      throw new IllegalArgumentException("contractNumber는 필수입니다.");
    }
    if (req.getPaymentDate() == null) {
      throw new IllegalArgumentException("paymentDate는 필수입니다.");
    }
    if (req.getPaymentAmount() == null || req.getPaymentAmount() <= 0) {
      throw new IllegalArgumentException("paymentAmount는 1 이상이어야 합니다.");
    }
  }

  private PaymentResponse toResponse(Payment p) {
    return PaymentResponse.builder()
        .id(p.getId())
        .contractNumber(p.getContractNumber())
        .customerName(p.getCustomerName())
        .vehicleNo(p.getVehicleNo())
        .paymentDate(p.getPaymentDate())
        .paymentAmount(p.getPaymentAmount())
        .paymentMethod(p.getPaymentMethod())
        .companyAccount(p.getCompanyAccount())
        .memo(p.getMemo())
        .voucherId(p.getVoucherId())
        .build();
  }

  private String blankToNull(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private String blankToEmpty(String s) {
    return s == null ? "" : s.trim();
  }
}
