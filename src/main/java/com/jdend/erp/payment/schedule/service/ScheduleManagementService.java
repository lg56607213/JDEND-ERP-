package com.jdend.erp.payment.schedule.service;

import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.payment.payment.entity.Payment;
import com.jdend.erp.payment.payment.repository.PaymentRepository;
import com.jdend.erp.payment.schedule.dto.ScheduleRowDto;
import com.jdend.erp.payment.schedule.dto.ScheduleSaveRequest;
import com.jdend.erp.payment.schedule.dto.ScheduleSearchResponse;
import com.jdend.erp.payment.schedule.entity.PaymentSchedule;
import com.jdend.erp.payment.schedule.repository.PaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleManagementService {

  private final ContractRepository contractRepo;
  private final PaymentScheduleRepository scheduleRepo;
  private final PaymentScheduleAutoGeneratorService autoGen;
  private final PaymentRepository paymentRepo;

  @Transactional
  public ScheduleSearchResponse getByVehicleNo(String vehicleNo) {
    if (vehicleNo == null || vehicleNo.trim().isEmpty()) {
      throw new RuntimeException("차량번호가 비었습니다.");
    }

    String vn = vehicleNo.trim();

    String contractNumber = contractRepo.findLatestContractNumberByVehicleNo(vn)
        .orElseThrow(() -> new RuntimeException("해당 차량번호의 계약을 찾을 수 없습니다: " + vn));

    List<PaymentSchedule> list = scheduleRepo.findByContractNumberOrderByInstallmentNoAsc(contractNumber);

    if (list.isEmpty()) {
      Contract c = contractRepo.findByContractNumber(contractNumber)
          .orElseThrow(() -> new RuntimeException("계약 없음: " + contractNumber));

      autoGen.ensureGenerated(c);
      list = scheduleRepo.findByContractNumberOrderByInstallmentNoAsc(contractNumber);
    }

    // ✅ 계약 전체 수납내역 조회
    List<Payment> payments = paymentRepo.findByContractNumberOrderByPaymentDateAscIdAsc(contractNumber);

    long totalPaidAmount = 0L;
    for (Payment p : payments) {
      if (p.getPaymentAmount() != null && p.getPaymentAmount() > 0) {
        totalPaidAmount += p.getPaymentAmount();
      }
    }

    long remainingPaid = totalPaidAmount;
    LocalDate today = LocalDate.now();

    List<ScheduleRowDto> out = new ArrayList<>();
    for (PaymentSchedule ps : list) {
      long rent = nz(ps.getRentAmount());

      long paidForThisRow = Math.min(remainingPaid, rent);
      if (paidForThisRow < 0) paidForThisRow = 0L;

      remainingPaid = Math.max(remainingPaid - paidForThisRow, 0L);

      long unpaid = Math.max(rent - paidForThisRow, 0L);

      LocalDate dueDate = ps.getPaymentDate();
      long receivable = (dueDate != null && dueDate.isBefore(today)) ? unpaid : 0L;

      String status;
      if (unpaid == 0L) {
        status = "완납";
      } else if (paidForThisRow > 0L) {
        status = "부분수납";
      } else if (dueDate != null && dueDate.isBefore(today)) {
        status = "미납";
      } else {
        status = "예정";
      }

      out.add(ScheduleRowDto.builder()
          .no(ps.getInstallmentNo())
          .startDate(toStr(ps.getBillStartDate()))
          .endDate(toStr(ps.getBillEndDate()))
          .taxDate(toStr(ps.getTaxInvoiceDate()))
          .payDate(toStr(ps.getPaymentDate()))
          .rent(ps.getRentAmount())
          .principal(ps.getPrincipalAmount())
          .interest(ps.getInterestAmount())
          .balance(ps.getRemainingPrincipal())
          .paid(paidForThisRow)
          .unpaid(unpaid)
          .receivable(receivable)
          .status(status)
          .build());
    }

    return ScheduleSearchResponse.builder()
        .vehicleNo(vn)
        .contractNumber(contractNumber)
        .schedule(out)
        .build();
  }

  @Transactional
  public void save(ScheduleSaveRequest req) {
    if (req == null || req.getContractNumber() == null || req.getContractNumber().isBlank()) {
      throw new RuntimeException("contractNumber가 필요합니다.");
    }
    if (req.getSchedule() == null) {
      throw new RuntimeException("schedule이 필요합니다.");
    }

    Contract contract = contractRepo.findByContractNumber(req.getContractNumber())
        .orElseThrow(() -> new RuntimeException("계약 없음: " + req.getContractNumber()));

    for (ScheduleRowDto row : req.getSchedule()) {
      if (row.getNo() == null) continue;

      PaymentSchedule ps = scheduleRepo.findByContractNumberAndInstallmentNo(req.getContractNumber(), row.getNo())
          .orElseGet(() -> PaymentSchedule.builder()
              .contract(contract)
              .contractNumber(req.getContractNumber())
              .installmentNo(row.getNo())
              .build()
          );

      ps.setBillStartDate(parseDate(row.getStartDate()));
      ps.setBillEndDate(parseDate(row.getEndDate()));
      ps.setTaxInvoiceDate(parseDate(row.getTaxDate()));
      ps.setPaymentDate(parseDate(row.getPayDate()));

      // ✅ 금액은 기존 유지
      ps.setRentAmount(row.getRent());
      ps.setPrincipalAmount(row.getPrincipal());
      ps.setInterestAmount(row.getInterest());
      ps.setRemainingPrincipal(row.getBalance());

      scheduleRepo.save(ps);
    }
  }

  private static long nz(Long v) {
    return v == null ? 0L : v;
  }

  private static String toStr(LocalDate d) {
    return d == null ? null : d.toString();
  }

  private static LocalDate parseDate(String s) {
    if (s == null || s.isBlank()) return null;
    return LocalDate.parse(s);
  }
}