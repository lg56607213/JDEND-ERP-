package com.jdend.erp.vehicle.service;

import com.jdend.erp.vehicle.dto.LoanVehiclePickerRowDto;
import com.jdend.erp.vehicle.dto.VehicleLoanScheduleResponse;
import com.jdend.erp.vehicle.dto.VehicleLoanScheduleRowDto;
import com.jdend.erp.vehicle.dto.VehicleLoanScheduleSaveRequest;
import com.jdend.erp.vehicle.entity.VehicleLoan;
import com.jdend.erp.vehicle.entity.VehicleLoanScheduleLine;
import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.repository.VehicleLoanRepository;
import com.jdend.erp.vehicle.repository.VehicleLoanScheduleLineRepository;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleLoanScheduleAdjustService {

    private final VehicleOrderRepository vehicleOrderRepository;
    private final VehicleLoanRepository vehicleLoanRepository;
    private final VehicleLoanScheduleLineRepository lineRepository;

    @Transactional(readOnly = true)
    public List<LoanVehiclePickerRowDto> searchLoanVehiclePicker(String kw) {
        String keyword = kw == null ? "" : kw.trim();

        return vehicleLoanRepository.searchLoanVehiclePicker(keyword).stream()
                .map(loan -> LoanVehiclePickerRowDto.builder()
                        .loanId(loan.getId())
                        .vehicleMgmtNo(loan.getVehicleOrder() != null ? loan.getVehicleOrder().getVehicleMgmtNo() : null)
                        .vehicleNo(loan.getVehicleOrder() != null ? loan.getVehicleOrder().getVehicleNo() : null)
                        .carModel(loan.getVehicleOrder() != null ? loan.getVehicleOrder().getCarModel() : null)
                        .financeName(loan.getFinanceName())
                        .build())
                .toList();
    }

    // ✅ readOnly 금지: 최초 조회 시 insert 발생 가능
    @Transactional
    public VehicleLoanScheduleResponse getByVehicleNo(String vehicleNo) {
        VehicleOrder order = vehicleOrderRepository.findByVehicleNoNormalized(vehicleNo)
                .orElseThrow(() -> new RuntimeException("차량번호를 찾을 수 없습니다: " + vehicleNo));

        VehicleLoan loan = vehicleLoanRepository.findTopByVehicleOrder_IdOrderByIdDesc(order.getId())
                .orElseThrow(() -> new RuntimeException("차입금 정보가 없습니다. 먼저 차입금스케줄등록을 해주세요."));

        List<VehicleLoanScheduleLine> original =
                lineRepository.findByLoanIdAndScheduleTypeOrderByInstallmentNoAsc(loan.getId(), "ORIGINAL");

        List<VehicleLoanScheduleLine> adjusted =
                lineRepository.findByLoanIdAndScheduleTypeOrderByInstallmentNoAsc(loan.getId(), "ADJUSTED");

        if (original.isEmpty()) {
            List<VehicleLoanScheduleRowDto> generated = generateSchedule(loan);
            saveLines(loan.getId(), "ORIGINAL", generated);
            saveLines(loan.getId(), "ADJUSTED", generated);

            original = lineRepository.findByLoanIdAndScheduleTypeOrderByInstallmentNoAsc(loan.getId(), "ORIGINAL");
            adjusted = lineRepository.findByLoanIdAndScheduleTypeOrderByInstallmentNoAsc(loan.getId(), "ADJUSTED");
        }

        if (adjusted.isEmpty()) {
            adjusted = original;
        }

        return VehicleLoanScheduleResponse.builder()
                .vehicleNo(order.getVehicleNo())
                .vehicleMgmtNo(order.getVehicleMgmtNo())
                .financeName(loan.getFinanceName())
                .loanPrincipal(loan.getLoanPrincipal())
                .loanInterest(loan.getLoanInterest())
                .repaymentPeriod(loan.getRepaymentPeriod())
                .monthlyPayment(loan.getMonthlyPayment())
                .paymentDay(loan.getPaymentDay())
                .repaymentAccount(loan.getRepaymentAccount())
                .originalSchedule(toDtoList(original))
                .adjustedSchedule(toDtoList(adjusted))
                .build();
    }

    @Transactional
    public int saveAdjustedSchedule(VehicleLoanScheduleSaveRequest req) {
        if (req == null || req.getVehicleNo() == null || req.getVehicleNo().isBlank()) {
            throw new RuntimeException("차량번호가 없습니다.");
        }

        VehicleOrder order = vehicleOrderRepository.findByVehicleNoNormalized(req.getVehicleNo())
                .orElseThrow(() -> new RuntimeException("차량번호를 찾을 수 없습니다: " + req.getVehicleNo()));

        VehicleLoan loan = vehicleLoanRepository.findTopByVehicleOrder_IdOrderByIdDesc(order.getId())
                .orElseThrow(() -> new RuntimeException("차입금 정보가 없습니다."));

        List<VehicleLoanScheduleRowDto> adjusted = req.getAdjustedSchedule();
        if (adjusted == null || adjusted.isEmpty()) {
            throw new RuntimeException("저장할 스케줄이 없습니다.");
        }

        List<VehicleLoanScheduleRowDto> recalculated = recalculateAdjustedRows(loan, adjusted);

        lineRepository.deleteByLoanIdAndScheduleType(loan.getId(), "ADJUSTED");
        saveLines(loan.getId(), "ADJUSTED", recalculated);

        return recalculated.size();
    }

    private void saveLines(Long loanId, String type, List<VehicleLoanScheduleRowDto> rows) {
        List<VehicleLoanScheduleLine> entities = new ArrayList<>();

        for (VehicleLoanScheduleRowDto row : rows) {
            entities.add(
                    VehicleLoanScheduleLine.builder()
                            .loanId(loanId)
                            .scheduleType(type)
                            .installmentNo(row.getInstallmentNo())
                            .paymentDate(row.getPaymentDate())
                            .monthlyPayment(row.getMonthlyPayment())
                            .principalAmount(row.getPrincipalAmount())
                            .interestAmount(row.getInterestAmount())
                            .remainingPrincipal(row.getRemainingPrincipal())
                            .repaymentAccount(row.getRepaymentAccount())
                            .build()
            );
        }

        lineRepository.saveAll(entities);
    }

    private List<VehicleLoanScheduleRowDto> toDtoList(List<VehicleLoanScheduleLine> rows) {
        return rows.stream()
                .map(r -> VehicleLoanScheduleRowDto.builder()
                        .installmentNo(r.getInstallmentNo())
                        .paymentDate(r.getPaymentDate())
                        .monthlyPayment(r.getMonthlyPayment())
                        .principalAmount(r.getPrincipalAmount())
                        .interestAmount(r.getInterestAmount())
                        .remainingPrincipal(r.getRemainingPrincipal())
                        .repaymentAccount(r.getRepaymentAccount())
                        .build())
                .toList();
    }

    private List<VehicleLoanScheduleRowDto> generateSchedule(VehicleLoan loan) {
        List<VehicleLoanScheduleRowDto> list = new ArrayList<>();

        long principal = nvl(loan.getLoanPrincipal());
        double annualRate = loan.getLoanInterest() == null ? 0.0 : loan.getLoanInterest();
        int months = loan.getRepaymentPeriod() == null ? 0 : loan.getRepaymentPeriod();
        int paymentDay = loan.getPaymentDay() == null ? 25 : loan.getPaymentDay();
        String repaymentMethod = loan.getRepaymentMethod() == null ? "원리금균등상환" : loan.getRepaymentMethod();
        String repaymentAccount = loan.getRepaymentAccount();
        long monthlyPayment = parseMoney(loan.getMonthlyPayment());

        if (principal <= 0 || months <= 0 || loan.getStartDate() == null) {
            return list;
        }

        double monthlyRate = annualRate / 100.0 / 12.0;
        long remaining = principal;

        list.add(VehicleLoanScheduleRowDto.builder()
                .installmentNo(0)
                .paymentDate(loan.getStartDate())
                .monthlyPayment(null)
                .principalAmount(principal)
                .interestAmount(null)
                .remainingPrincipal(principal)
                .repaymentAccount(repaymentAccount)
                .build());

        for (int i = 1; i <= months; i++) {
            LocalDate paymentDate = calcPaymentDate(loan.getStartDate(), i, paymentDay);

            long interest;
            long principalAmount;
            long payAmount;

            if ("원금균등상환".equals(repaymentMethod)) {
                principalAmount = Math.round((double) principal / months);
                interest = Math.round(remaining * monthlyRate);
                payAmount = principalAmount + interest;
            } else if ("만기일시상환".equals(repaymentMethod)) {
                interest = Math.round(remaining * monthlyRate);
                principalAmount = (i == months) ? remaining : 0L;
                payAmount = principalAmount + interest;
            } else {
                payAmount = monthlyPayment;
                interest = Math.round(remaining * monthlyRate);
                principalAmount = payAmount - interest;
            }

            if (principalAmount < 0) principalAmount = 0;

            if (i == months || principalAmount > remaining) {
                principalAmount = remaining;
                payAmount = principalAmount + Math.round(remaining * monthlyRate);
                interest = payAmount - principalAmount;
            }

            remaining -= principalAmount;
            if (remaining < 0) remaining = 0;

            list.add(VehicleLoanScheduleRowDto.builder()
                    .installmentNo(i)
                    .paymentDate(paymentDate)
                    .monthlyPayment(payAmount)
                    .principalAmount(principalAmount)
                    .interestAmount(interest)
                    .remainingPrincipal(remaining == 0 ? null : remaining)
                    .repaymentAccount(repaymentAccount)
                    .build());
        }

        return list;
    }

    private List<VehicleLoanScheduleRowDto> recalculateAdjustedRows(VehicleLoan loan, List<VehicleLoanScheduleRowDto> adjusted) {
        if (adjusted == null || adjusted.isEmpty()) return List.of();

        List<VehicleLoanScheduleRowDto> result = new ArrayList<>();
        for (VehicleLoanScheduleRowDto row : adjusted) {
            result.add(VehicleLoanScheduleRowDto.builder()
                    .installmentNo(row.getInstallmentNo())
                    .paymentDate(row.getPaymentDate())
                    .monthlyPayment(row.getMonthlyPayment())
                    .principalAmount(row.getPrincipalAmount())
                    .interestAmount(row.getInterestAmount())
                    .remainingPrincipal(row.getRemainingPrincipal())
                    .repaymentAccount(row.getRepaymentAccount() != null ? row.getRepaymentAccount() : loan.getRepaymentAccount())
                    .build());
        }

        long openingPrincipal = nvl(loan.getLoanPrincipal());
        double monthlyRate = (loan.getLoanInterest() == null ? 0.0 : loan.getLoanInterest()) / 100.0 / 12.0;

        if (!result.isEmpty()) {
            VehicleLoanScheduleRowDto row0 = result.get(0);
            row0.setInstallmentNo(0);
            if (row0.getPaymentDate() == null) row0.setPaymentDate(loan.getStartDate());
            row0.setMonthlyPayment(null);
            row0.setPrincipalAmount(openingPrincipal);
            row0.setInterestAmount(null);
            row0.setRemainingPrincipal(openingPrincipal);
            if (row0.getRepaymentAccount() == null) row0.setRepaymentAccount(loan.getRepaymentAccount());
        }

        long prevBalance = openingPrincipal;

        for (int i = 1; i < result.size(); i++) {
            VehicleLoanScheduleRowDto row = result.get(i);

            long pay = nvl(row.getMonthlyPayment());
            long interest = Math.round(prevBalance * monthlyRate);
            long principal = pay - interest;

            if (principal < 0) principal = 0;
            if (principal > prevBalance) principal = prevBalance;

            long remaining = prevBalance - principal;

            if (i == result.size() - 1) {
                principal = prevBalance;
                pay = principal + interest;
                remaining = 0;
                row.setMonthlyPayment(pay);
            }

            row.setPrincipalAmount(principal);
            row.setInterestAmount(interest);
            row.setRemainingPrincipal(remaining == 0 ? null : remaining);
            if (row.getRepaymentAccount() == null) row.setRepaymentAccount(loan.getRepaymentAccount());

            prevBalance = remaining;
        }

        return result;
    }

    private LocalDate calcPaymentDate(LocalDate startDate, int plusMonths, int paymentDay) {
        LocalDate base = startDate.plusMonths(plusMonths);
        int day = Math.min(paymentDay, base.lengthOfMonth());
        return base.withDayOfMonth(day);
    }

    private long parseMoney(String value) {
        if (value == null || value.isBlank()) return 0L;
        String onlyNum = value.replaceAll("[^0-9]", "");
        if (onlyNum.isBlank()) return 0L;
        return Long.parseLong(onlyNum);
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }
}