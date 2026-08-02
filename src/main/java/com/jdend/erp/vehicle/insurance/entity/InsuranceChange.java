package com.jdend.erp.vehicle.insurance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_changes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class InsuranceChange {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "insurance_id", nullable = false)
  private Long insuranceId;

  @Column(name = "change_type", nullable = false, length = 10)
  private String changeType;

  @Column(name = "change_reason", length = 500)
  private String changeReason;

  @Column(name = "additional_premium")
  private Long additionalPremium;

  @Column(name = "refund_premium")
  private Long refundPremium;

  @Column(name = "voucher_date")
  private LocalDate voucherDate;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;
}
