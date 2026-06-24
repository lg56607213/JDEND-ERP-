package com.jdend.erp.vehicle.advance.entity;

import com.jdend.erp.vehicle.entity.VehicleOrder;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name="vehicle_advances")
public class VehicleAdvance {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="vehicle_order_id", nullable=false)
  private VehicleOrder vehicleOrder;

  @Column(name="item_name", nullable=false, length=50)
  private String itemName;

  @Column(name="advance_amount", nullable=false)
  private Long advanceAmount;

  @Column(name="supply_amount", nullable=false)
  private Long supplyAmount;

  @Column(name="vat_amount", nullable=false)
  private Long vatAmount;

  @Column(name="voucher_date")
  private LocalDate voucherDate;

  @Column(name="payment_method", length=30)
  private String paymentMethod;

  @Column(name="remark", length=255)
  private String remark;

  @Column(name="created_at")
  private LocalDateTime createdAt;

  @Column(name="updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    if (advanceAmount == null) advanceAmount = 0L;
    if (supplyAmount == null || vatAmount == null) {
      // 선급금액 = 공급가액 + 부가세 (10%)
      long supply = Math.round(advanceAmount / 1.1);
      long vat = advanceAmount - supply;
      supplyAmount = supply;
      vatAmount = vat;
    }
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
