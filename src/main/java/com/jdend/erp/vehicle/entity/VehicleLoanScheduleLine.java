package com.jdend.erp.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_loan_schedule_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleLoanScheduleLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "schedule_type", nullable = false, length = 20)
    private String scheduleType; // ORIGINAL / ADJUSTED

    @Column(name = "installment_no", nullable = false)
    private Integer installmentNo;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "monthly_payment")
    private Long monthlyPayment;

    @Column(name = "principal_amount")
    private Long principalAmount;

    @Column(name = "interest_amount")
    private Long interestAmount;

    @Column(name = "remaining_principal")
    private Long remainingPrincipal;

    @Column(name = "repayment_account", length = 100)
    private String repaymentAccount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}