package com.jdend.erp.payment.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "payments")
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name="contract_id", nullable=false)
  private Long contractId;

  @Column(name="contract_number", nullable=false, length=30)
  private String contractNumber;

  @Column(name="customer_id")
  private Long customerId;

  @Column(name="customer_number", length=30)
  private String customerNumber;

  @Column(name="customer_name", length=100)
  private String customerName;

  @Column(name="vehicle_no", length=30)
  private String vehicleNo;

  @Column(name="payment_date", nullable=false)
  private LocalDate paymentDate;

  @Column(name="payment_amount", nullable=false)
  private Long paymentAmount;

  @Column(name="payment_method", length=30)
  private String paymentMethod;

  @Column(name="company_account", length=30)
  private String companyAccount;

  @Column(name="memo", length=255)
  private String memo;

  @Column(name="created_at", insertable=false, updatable=false)
  private LocalDateTime createdAt;

  @Column(name="updated_at", insertable=false, updatable=false)
  private LocalDateTime updatedAt;
}