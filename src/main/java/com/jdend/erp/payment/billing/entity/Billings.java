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
@Table(name = "billings")
public class Billings {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "billing_no", nullable = false, unique = true, length = 50)
  private String billingNo;

  @Column(name = "contract_number", length = 50)
  private String contractNumber;

  @Column(name = "billing_type", length = 20)
  private String billingType; // BATCH / INDIVIDUAL

  @Column(name = "billing_date")
  private LocalDate billingDate;

  @Column(name = "tax_start_date")
  private LocalDate taxStartDate;

  @Column(name = "tax_end_date")
  private LocalDate taxEndDate;

  @Column(name = "tax_invoice_date")
  private LocalDate taxInvoiceDate;

  @Column(name = "payment_date")
  private LocalDate paymentDate;

  @Column(name = "rent_amount")
  private Long rentAmount;

  @Column(name = "customer_id")
  private Long customerId;

  @Column(name = "customer_number", length = 50)
  private String customerNumber;

  @Column(name = "customer_name", length = 200)
  private String customerName;

  @Column(name = "email", length = 200)
  private String email;

  @Column(name = "overdue_type", length = 20)
  private String overdueType; // ALL / EXCLUDE / ONLY

  @Column(name = "status", length = 20)
  private String status; // CREATED / ...

  @Column(name = "total_amount")
  private Long totalAmount;

  @Column(name = "memo", length = 255)
  private String memo;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}