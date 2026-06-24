package com.jdend.erp.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name="vehicle_loans")
public class VehicleLoan {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // FK: vehicle_order_id -> vehicle_orders.id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="vehicle_order_id", nullable=false)
  private VehicleOrder vehicleOrder;

  @Column(name="vehicle_mgmt_no", nullable=false, length=20)
  private String vehicleMgmtNo;

  @Column(name="loan_type", nullable=false, length=20)
  private String loanType;

  @Column(name="loan_principal", nullable=false)
  private Long loanPrincipal;

  @Column(name="loan_interest", nullable=false)
  private Double loanInterest;

  @Column(name="finance_name", nullable=false, length=50)
  private String financeName;

  @Column(name="repayment_method", nullable=false, length=30)
  private String repaymentMethod;

  @Column(name="repayment_period", nullable=false)
  private Integer repaymentPeriod;

  @Column(name="down_payment")
  private Long downPayment;

  @Column(name="start_date", nullable=false)
  private LocalDate startDate;

  @Column(name="end_date")
  private LocalDate endDate;

  @Column(name="deposit")
  private Long deposit;

  @Column(name="payment_day", nullable=false)
  private Integer paymentDay;

  @Column(name="monthly_payment", length=30)
  private String monthlyPayment;

  // ✅ 너 DB에 이미 있음 (SHOW COLUMNS 결과 기준)
  @Column(name="repayment_account", length=50)
  private String repaymentAccount;

  // ✅ 너 DB에 이미 있음 (remaining_principal)
  @Column(name="remaining_principal")
  private Long remainingPrincipal;

  // ✅ 너 DB에 이미 있음 (last_payment_date)
  @Column(name="last_payment_date")
  private LocalDate lastPaymentDate;

  // ✅ 이번에 추가할 컬럼
@Column(name="is_terminated", nullable=false)
private Boolean terminated;


  @Column(name="terminated_at")
  private LocalDateTime terminatedAt;

  @Column(name="created_at")
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) createdAt = LocalDateTime.now();
    if (downPayment == null) downPayment = 0L;
    if (deposit == null) deposit = 0L;

    if (remainingPrincipal == null) remainingPrincipal = loanPrincipal; // 최초는 원금 = 잔액
    if (terminated == null) terminated = false;
  }
}
