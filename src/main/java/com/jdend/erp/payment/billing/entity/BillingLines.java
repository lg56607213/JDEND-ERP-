package com.jdend.erp.payment.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "billing_lines")
public class BillingLines {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "billing_id", nullable = false)
  private Long billingId;

  @Column(name = "contract_id")
  private Long contractId;

  @Column(name = "contract_number", length = 50)
  private String contractNumber;

  @Column(name = "schedule_id")
  private Long scheduleId;

  @Column(name = "installment_no")
  private Integer installmentNo;

  @Column(name = "tax_invoice_date")
  private LocalDate taxInvoiceDate;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "rent_amount")
  private Long rentAmount;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}