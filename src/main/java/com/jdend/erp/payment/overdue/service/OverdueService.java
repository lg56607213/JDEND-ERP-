package com.jdend.erp.payment.overdue.service;

import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.payment.overdue.dto.OverdueRowResponse;
import com.jdend.erp.payment.payment.entity.Payment;
import com.jdend.erp.payment.payment.repository.PaymentRepository;
import com.jdend.erp.payment.schedule.entity.PaymentSchedule;
import com.jdend.erp.payment.schedule.repository.PaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OverdueService {

    private final ContractRepository contractRepo;
    private final PaymentScheduleRepository scheduleRepo;
    private final PaymentRepository paymentRepo;

    @Transactional(readOnly = true)
    public List<OverdueRowResponse> overdueList() {
        LocalDate today = LocalDate.now();
        List<Contract> activeContracts = contractRepo.findAllActiveWithCustomer();
        List<OverdueRowResponse> result = new ArrayList<>();

        for (Contract c : activeContracts) {
            String contractNumber = c.getContractNumber();
            List<PaymentSchedule> schedules =
                    scheduleRepo.findByContractNumberOrderByInstallmentNoAsc(contractNumber);
            if (schedules.isEmpty()) continue;

            List<Payment> payments =
                    paymentRepo.findByContractNumberOrderByPaymentDateAscIdAsc(contractNumber);

            long totalPaid = 0L;
            for (Payment p : payments) {
                if (p.getPaymentAmount() != null && p.getPaymentAmount() > 0)
                    totalPaid += p.getPaymentAmount();
            }

            long remaining = totalPaid;
            String customerName = (c.getCustomer() != null) ? c.getCustomer().getCustomerName() : null;

            for (PaymentSchedule ps : schedules) {
                long rent = ps.getRentAmount() != null ? ps.getRentAmount() : 0L;
                long paid = Math.max(0, Math.min(remaining, rent));
                remaining = Math.max(0, remaining - paid);
                long unpaid = Math.max(0, rent - paid);

                LocalDate dueDate = ps.getPaymentDate();
                if (dueDate != null && dueDate.isBefore(today) && unpaid > 0) {
                    result.add(OverdueRowResponse.builder()
                            .contractNumber(contractNumber)
                            .vehicleNo(c.getVehicleNo())
                            .customerName(customerName)
                            .installmentNo(ps.getInstallmentNo())
                            .paymentDate(dueDate)
                            .rentAmount(rent)
                            .paidAmount(paid)
                            .unpaidAmount(unpaid)
                            .overdueDays((int) ChronoUnit.DAYS.between(dueDate, today))
                            .build());
                }
            }
        }
        return result;
    }
}
