package com.jdend.erp.vehicle.service;

import com.jdend.erp.accounting.settings.service.OtherAccountSettingsService;
import com.jdend.erp.accounting.voucher.dto.VoucherCreateRequest;
import com.jdend.erp.accounting.voucher.service.VoucherService;
import com.jdend.erp.vehicle.dto.*;
import com.jdend.erp.vehicle.entity.VehicleLoan;
import com.jdend.erp.vehicle.entity.VehicleLoanVoucher;
import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.repository.VehicleLoanRepository;
import com.jdend.erp.vehicle.repository.VehicleLoanVoucherRepository;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleLoanService {

    private final VehicleLoanRepository loanRepo;
    private final VehicleLoanVoucherRepository voucherRepo;
    private final VehicleOrderRepository vehicleOrderRepo;
    private final VoucherService voucherService;
    private final OtherAccountSettingsService accountSettings;

    @Transactional(readOnly = true)
    public List<VehicleLoanListItemResponse> list(
            String contractNo,
            String vehicleNo,
            String financeName,
            Boolean terminated
    ) {
        String kwVehicle = vehicleNo == null ? "" : vehicleNo.trim();
        String kwFinance = financeName == null ? "" : financeName.trim();
        String kwContract = contractNo == null ? "" : contractNo.trim();

        List<VehicleLoan> loans = loanRepo.search(kwVehicle, kwFinance, terminated);

        if (!kwContract.isBlank()) {
            loans = loans.stream().filter(l -> {
                VehicleOrder o = l.getVehicleOrder();
                String maker = (o == null || o.getMakerContractNo() == null)
                        ? "" : o.getMakerContractNo();
                return maker.toLowerCase().contains(kwContract.toLowerCase());
            }).toList();
        }

        loans = loans.stream()
                .sorted(Comparator.comparing(VehicleLoan::getId).reversed())
                .toList();

        return loans.stream().map(this::toItem).toList();
    }

    @Transactional(readOnly = true)
    public VehicleLoanDetailResponse detail(Long id) {
        VehicleLoan loan = loanRepo.findDetail(id)
                .orElseThrow(() -> new RuntimeException("차입금 없음 id=" + id));

        VehicleOrder o = loan.getVehicleOrder();

        List<VehicleLoanVoucherRowResponse> recent =
                voucherRepo.findTop6ByLoan_IdOrderByVoucherDateDescIdDesc(id)
                        .stream()
                        .map(v -> VehicleLoanVoucherRowResponse.builder()
                                .id(v.getId())
                                .voucherDate(v.getVoucherDate())
                                .amount(v.getAmount())
                                .memo(v.getMemo())
                                .installmentNo(v.getInstallmentNo())
                                .voucherCreated(v.isVoucherCreated())
                                .build())
                        .toList();

        return VehicleLoanDetailResponse.builder()
                .id(loan.getId())
                .makerContractNo(o != null ? o.getMakerContractNo() : null)
                .vehicleMgmtNo(loan.getVehicleMgmtNo())
                .vehicleNo(o != null ? o.getVehicleNo() : null)
                .loanType(loan.getLoanType())
                .loanPrincipal(loan.getLoanPrincipal())
                .loanInterest(loan.getLoanInterest())
                .financeName(loan.getFinanceName())
                .repaymentMethod(loan.getRepaymentMethod())
                .repaymentPeriod(loan.getRepaymentPeriod())
                .paymentDay(loan.getPaymentDay())
                .monthlyPayment(loan.getMonthlyPayment())
                .repaymentAccount(loan.getRepaymentAccount())
                .remainingPrincipal(loan.getRemainingPrincipal())
                .lastPaymentDate(loan.getLastPaymentDate())
                .terminated(loan.getTerminated())
                .recentVouchers(recent)
                .build();
    }

    @Transactional(readOnly = true)
    public VehicleLoanDetailResponse findLatestByVehicleNo(String vehicleNo) {
        if (vehicleNo == null || vehicleNo.isBlank()) {
            return null;
        }

        List<VehicleLoan> list = loanRepo.findByVehicleNoNormalizedOrderByIdDesc(vehicleNo);
        if (list.isEmpty()) {
            return null;
        }

        VehicleLoan latest = list.get(0);
        return detail(latest.getId());
    }

    // 차입금 상환 현황
    @Transactional(readOnly = true)
    public List<VehicleLoanRepaymentStatusRowResponse> repaymentStatus(Long loanId) {
        VehicleLoan loan = loanRepo.findDetail(loanId)
                .orElseThrow(() -> new RuntimeException("차입금 없음 id=" + loanId));

        List<VehicleLoanVoucher> vouchers =
                voucherRepo.findByLoan_IdOrderByVoucherDateAscIdAsc(loanId);

        long totalPaid = vouchers.stream()
                .mapToLong(v -> v.getAmount() == null ? 0L : v.getAmount())
                .sum();

        long loanPrincipal = safeLong(loan.getLoanPrincipal());
        double annualRate = loan.getLoanInterest() == null ? 0.0 : loan.getLoanInterest();
        double monthlyRate = annualRate / 100.0 / 12.0;

        int period = loan.getRepaymentPeriod() == null ? 0 : loan.getRepaymentPeriod();
        if (period <= 0) {
            return List.of();
        }

        long monthlyPayment = parseMonthlyPayment(loan.getMonthlyPayment());
        if (monthlyPayment <= 0) {
            monthlyPayment = Math.round(loanPrincipal / (double) period);
        }

        LocalDate startDate = loan.getStartDate() == null ? LocalDate.now() : loan.getStartDate();
        int paymentDay = loan.getPaymentDay() == null ? 1 : loan.getPaymentDay();

        List<VehicleLoanRepaymentStatusRowResponse> result = new ArrayList<>();

        long prevBalance = loanPrincipal;
        long remainingPaid = totalPaid;

        for (int i = 1; i <= period; i++) {
            LocalDate paymentDate = startDate.plusMonths(i);
            int safeDay = Math.min(paymentDay, paymentDate.lengthOfMonth());
            paymentDate = paymentDate.withDayOfMonth(safeDay);

            long interest = Math.round(prevBalance * monthlyRate);
            long principal = monthlyPayment - interest;

            if (principal < 0) principal = 0;
            if (principal > prevBalance) principal = prevBalance;

            long scheduledAmount = principal + interest;

            if (i == period) {
                principal = prevBalance;
                scheduledAmount = principal + interest;
            }

            long paidForThisRow = 0L;

            if (remainingPaid >= scheduledAmount) {
                paidForThisRow = scheduledAmount;
                remainingPaid -= scheduledAmount;
            } else if (remainingPaid > 0) {
                paidForThisRow = remainingPaid;
                remainingPaid = 0;
            }

            long unpaid = Math.max(0L, scheduledAmount - paidForThisRow);
            long receivable = unpaid;

            String status;
            if (paidForThisRow >= scheduledAmount) {
                status = "완납";
            } else if (paidForThisRow > 0) {
                status = "부분수납";
            } else {
                status = "미납";
            }

            long remainingPrincipal = Math.max(0L, prevBalance - principal);

            result.add(VehicleLoanRepaymentStatusRowResponse.builder()
                    .installmentNo(i)
                    .paymentDate(paymentDate)
                    .monthlyPayment(scheduledAmount)
                    .principalAmount(principal)
                    .interestAmount(interest)
                    .remainingPrincipal(remainingPrincipal)
                    .paidAmount(paidForThisRow)
                    .unpaidAmount(unpaid)
                    .receivableAmount(receivable)
                    .repaymentAccount(loan.getRepaymentAccount())
                    .status(status)
                    .build());

            prevBalance = remainingPrincipal;
        }

        return result;
    }

    @Transactional
    public VehicleLoanListItemResponse create(VehicleLoanCreateRequest req) {
        if (req.vehicleNo == null || req.vehicleNo.isBlank()) {
            throw new RuntimeException("차량번호는 필수입니다.");
        }

        VehicleOrder order = vehicleOrderRepo.findByVehicleNoNormalized(req.vehicleNo)
                .orElseThrow(() -> new RuntimeException("차량번호를 찾을 수 없습니다."));

        List<VehicleLoan> existing = loanRepo.findByVehicleNoNormalizedOrderByIdDesc(req.vehicleNo);
        if (!existing.isEmpty()) {
            VehicleLoan latest = existing.get(0);
            if (latest.getTerminated() == null || !latest.getTerminated()) {
                throw new RuntimeException("이미 등록된 차입금이 있습니다. 기존 건을 수정해주세요.");
            }
        }

        if (req.loanPrincipal == null || req.loanPrincipal <= 0) {
            throw new RuntimeException("차입금액은 1원 이상이어야 합니다.");
        }

        if (req.repaymentPeriod == null || req.repaymentPeriod <= 0) {
            throw new RuntimeException("상환기간은 1개월 이상이어야 합니다.");
        }

        if (req.paymentDay == null || req.paymentDay < 1 || req.paymentDay > 31) {
            throw new RuntimeException("상환일은 1~31입니다.");
        }

        LocalDate start = req.startDate != null ? req.startDate : LocalDate.now();
        LocalDate end = req.endDate != null
                ? req.endDate
                : start.plusMonths(req.repaymentPeriod);

        VehicleLoan loan = VehicleLoan.builder()
                .vehicleOrder(order)
                .vehicleMgmtNo(order.getVehicleMgmtNo())
                .loanType(req.loanType == null || req.loanType.isBlank() ? "차입금" : req.loanType)
                .loanPrincipal(req.loanPrincipal)
                .loanInterest(req.loanInterest == null ? 0.0 : req.loanInterest)
                .financeName(req.financeName)
                .repaymentMethod(req.repaymentMethod == null || req.repaymentMethod.isBlank() ? "원리금" : req.repaymentMethod)
                .repaymentPeriod(req.repaymentPeriod)
                .downPayment(req.downPayment == null ? 0L : req.downPayment)
                .deposit(req.deposit == null ? 0L : req.deposit)
                .startDate(start)
                .endDate(end)
                .paymentDay(req.paymentDay)
                .monthlyPayment(req.monthlyPayment)
                .repaymentAccount(req.repaymentAccount)
                .remainingPrincipal(req.loanPrincipal)
                .terminated(false)
                .build();

        loanRepo.save(loan);
        createLoanOpenVoucher(loan);

        return toItem(loan);
    }

    @Transactional
    public VehicleLoanListItemResponse update(Long id, VehicleLoanUpdateRequest req) {
        VehicleLoan loan = loanRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("차입금 없음"));

        if (req.financeName != null) loan.setFinanceName(req.financeName);
        if (req.loanPrincipal != null) loan.setLoanPrincipal(req.loanPrincipal);
        if (req.loanInterest != null) loan.setLoanInterest(req.loanInterest);
        if (req.repaymentPeriod != null) loan.setRepaymentPeriod(req.repaymentPeriod);
        if (req.paymentDay != null) loan.setPaymentDay(req.paymentDay);
        if (req.monthlyPayment != null) loan.setMonthlyPayment(req.monthlyPayment);
        if (req.repaymentAccount != null) loan.setRepaymentAccount(req.repaymentAccount);
        if (req.remainingPrincipal != null) loan.setRemainingPrincipal(req.remainingPrincipal);

        loanRepo.save(loan);
        return toItem(loan);
    }

    @Transactional
    public void terminate(List<Long> ids) {
        List<VehicleLoan> list = loanRepo.findAllById(ids);
        for (VehicleLoan l : list) {
            l.setTerminated(true);
            l.setTerminatedAt(LocalDateTime.now());
        }
        loanRepo.saveAll(list);
    }

    @Transactional
    public void createVouchers(VehicleLoanVoucherCreateRequest req) {
        if (req == null || req.loanIds == null || req.loanIds.isEmpty()) {
            throw new RuntimeException("전표를 생성할 차입금을 선택해주세요.");
        }

        if (req.voucherDate == null) {
            throw new RuntimeException("회계일자를 입력해주세요.");
        }

        if (req.amount == null || req.amount <= 0) {
            throw new RuntimeException("상환금액은 0보다 커야 합니다.");
        }

        List<VehicleLoan> loans = req.loanIds.stream()
                .map(id -> loanRepo.findDetail(id)
                        .orElseThrow(() -> new RuntimeException("차입금 없음 id=" + id)))
                .toList();

        boolean shouldCreateVoucher = req.createVoucher == null || req.createVoucher;

        for (VehicleLoan loan : loans) {
            VehicleOrder order = loan.getVehicleOrder();
            String vehicleNo = order != null ? order.getVehicleNo() : "";
            String contractNo = order != null ? order.getMakerContractNo() : null;

            String finalMemo = (req.memo == null || req.memo.isBlank())
                    ? ((vehicleNo == null || vehicleNo.isBlank() ? "" : vehicleNo + " ") + "차입금 상환")
                    : req.memo.trim();

            voucherRepo.save(
                    VehicleLoanVoucher.builder()
                            .loan(loan)
                            .voucherDate(req.voucherDate)
                            .amount(req.amount)
                            .memo(finalMemo)
                            .installmentNo(req.installmentNo)
                            .voucherCreated(shouldCreateVoucher)
                            .build()
            );

            if (!shouldCreateVoucher) {
                loan.setLastPaymentDate(req.voucherDate);
                loan.setRemainingPrincipal(
                        Math.max(0L, safeLong(loan.getRemainingPrincipal()) - req.amount)
                );
                continue;
            }

            String loanDebitAccount = accountSettings.getLoanDebit1Account();
            if (loanDebitAccount == null) loanDebitAccount = "장기차입금";
            String loanCreditAccount = accountSettings.getLoanCreditAccount();
            if (loanCreditAccount == null) loanCreditAccount = "보통예금";

            // BUG-02 fix: 이자금액이 있으면 차변을 원금(장기차입금) + 이자(이자비용)로 분리
            long interest = (req.interestAmount != null && req.interestAmount > 0) ? req.interestAmount : 0L;
            long principal = req.amount - interest;

            String loanDebitAccount2 = accountSettings.getLoanDebit2Account();

            List<VoucherCreateRequest.VoucherLineRequest> debitEntries;
            if (interest > 0 && principal > 0) {
                if (loanDebitAccount2 == null) {
                    log.warn("이자비용 계정 미설정으로 이자 분개를 건너뜁니다. 기타계정관리 > 차입금상환 이자 계정(차변 두 번째)을 설정하세요.");
                    debitEntries = List.of(
                            VoucherCreateRequest.VoucherLineRequest.builder()
                                    .account(loanDebitAccount)
                                    .amount(req.amount)
                                    .description("차입금 상환")
                                    .build()
                    );
                } else {
                    debitEntries = List.of(
                            VoucherCreateRequest.VoucherLineRequest.builder()
                                    .account(loanDebitAccount)
                                    .amount(principal)
                                    .description("차입금 원금 상환")
                                    .build(),
                            VoucherCreateRequest.VoucherLineRequest.builder()
                                    .account(loanDebitAccount2)
                                    .amount(interest)
                                    .description("차입금 이자")
                                    .build()
                    );
                }
            } else {
                debitEntries = List.of(
                        VoucherCreateRequest.VoucherLineRequest.builder()
                                .account(loanDebitAccount)
                                .amount(req.amount)
                                .description("차입금 상환")
                                .build()
                );
            }

            voucherService.create(
                    VoucherCreateRequest.builder()
                            .voucherDate(req.voucherDate)
                            .contractNumber(contractNo)
                            .vehicleNo(vehicleNo)
                            .memo(finalMemo)
                            .debitEntries(debitEntries)
                            .creditEntries(List.of(
                                    VoucherCreateRequest.VoucherLineRequest.builder()
                                            .account(loanCreditAccount)
                                            .amount(req.amount)
                                            .description("차입금 상환")
                                            .build()
                            ))
                            .build()
            );

            loan.setLastPaymentDate(req.voucherDate);
            loan.setRemainingPrincipal(
                    Math.max(0L, safeLong(loan.getRemainingPrincipal()) - req.amount)
            );
        }
    }

    private void createLoanOpenVoucher(VehicleLoan loan) {
        String debitAccount  = accountSettings.getLoanOpenDebitAccount();
        String creditAccount = accountSettings.getLoanOpenCreditAccount();
        if (debitAccount == null || creditAccount == null) {
            log.warn("차입금 개시 전표 생략: 기타계정관리 > 차입금 개시 전표의 차변/대변을 설정해주세요. loanId={}", loan.getId());
            return;
        }

        VehicleOrder order = loan.getVehicleOrder();
        String vehicleNo = order != null ? order.getVehicleNo() : "";
        String contractNo = order != null ? order.getMakerContractNo() : null;

        String memo = (vehicleNo == null || vehicleNo.isBlank())
                ? "차입금 등록"
                : vehicleNo + " 차입금 등록";

        voucherService.create(
                VoucherCreateRequest.builder()
                        .voucherDate(loan.getStartDate() != null ? loan.getStartDate() : LocalDate.now())
                        .contractNumber(contractNo)
                        .vehicleNo(vehicleNo)
                        .memo(memo)
                        .debitEntries(List.of(
                                VoucherCreateRequest.VoucherLineRequest.builder()
                                        .account(debitAccount)
                                        .amount(loan.getLoanPrincipal())
                                        .description("대출원금")
                                        .build()
                        ))
                        .creditEntries(List.of(
                                VoucherCreateRequest.VoucherLineRequest.builder()
                                        .account(creditAccount)
                                        .amount(loan.getLoanPrincipal())
                                        .description("대출원금")
                                        .build()
                        ))
                        .build()
        );
    }

    private long parseMonthlyPayment(String value) {
        if (value == null || value.isBlank()) return 0L;

        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return 0L;

        try {
            return Long.parseLong(digits);
        } catch (Exception e) {
            return 0L;
        }
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private VehicleLoanListItemResponse toItem(VehicleLoan loan) {
        VehicleOrder o = loan.getVehicleOrder();

        return VehicleLoanListItemResponse.builder()
                .id(loan.getId())
                .vehicleMgmtNo(loan.getVehicleMgmtNo())
                .makerContractNo(o != null ? o.getMakerContractNo() : null)
                .vehicleNo(o != null ? o.getVehicleNo() : null)
                .loanType(loan.getLoanType())
                .loanPrincipal(loan.getLoanPrincipal())
                .loanInterest(loan.getLoanInterest())
                .financeName(loan.getFinanceName())
                .repaymentMethod(loan.getRepaymentMethod())
                .repaymentPeriod(loan.getRepaymentPeriod())
                .paymentDay(loan.getPaymentDay())
                .monthlyPayment(loan.getMonthlyPayment())
                .repaymentAccount(loan.getRepaymentAccount())
                .remainingPrincipal(loan.getRemainingPrincipal())
                .lastPaymentDate(loan.getLastPaymentDate())
                .terminated(loan.getTerminated())
                .build();
    }
}