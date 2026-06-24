package com.jdend.erp.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name="vehicle_loan_vouchers")
public class VehicleLoanVoucher {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="loan_id", nullable=false)
  private VehicleLoan loan;

  @Column(name="voucher_date", nullable=false)
  private LocalDate voucherDate;

  @Column(name="amount", nullable=false)
  private Long amount;

  @Column(name="memo", length=255)
  private String memo;

  @Column(name="created_at")
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) createdAt = LocalDateTime.now();
    if (amount == null) amount = 0L;
    if (voucherDate == null) voucherDate = LocalDate.now();
  }
}
