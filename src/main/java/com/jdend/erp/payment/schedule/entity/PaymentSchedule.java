package com.jdend.erp.payment.schedule.entity;

import com.jdend.erp.contract.entity.Contract;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_schedules",
    uniqueConstraints = @UniqueConstraint(name="uk_payment_schedules_contract_installment",
        columnNames = {"contract_number", "installment_no"})
)
public class PaymentSchedule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="contract_id")
  private Contract contract;

  @Column(name="contract_number", nullable=false, length=30)
  private String contractNumber;

  @Column(name="installment_no", nullable=false)
  private Integer installmentNo;

  @Column(name="bill_start_date")
  private LocalDate billStartDate;

  @Column(name="bill_end_date")
  private LocalDate billEndDate;

  @Column(name="tax_invoice_date")
  private LocalDate taxInvoiceDate;

  @Column(name="payment_date")
  private LocalDate paymentDate;

  @Column(name="rent_amount")
  private Long rentAmount;

  @Column(name="principal_amount")
  private Long principalAmount;

  @Column(name="interest_amount")
  private Long interestAmount;

  @Column(name="remaining_principal")
  private Long remainingPrincipal;

  @Column(name="created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name="updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}