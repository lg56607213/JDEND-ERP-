package com.jdend.erp.payment.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "payment_schedules")
public class PaymentSchedules {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "contract_id")
  private Long contractId;

  @Column(name = "contract_number")
  private String contractNumber;

  @Column(name = "installment_no")
  private Integer installmentNo;

  @Column(name = "tax_invoice_date")
  private LocalDate taxInvoiceDate;

  @Column(name = "payment_date")
  private LocalDate paymentDate; // 수납일(없으면 null)

  @Column(name = "rent_amount")
  private Long rentAmount;
}