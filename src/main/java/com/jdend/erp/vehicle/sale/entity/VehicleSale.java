package com.jdend.erp.vehicle.sale.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_sales")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleSale {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "vehicle_order_id")
  private Long vehicleOrderId;

  @Column(name = "vehicle_mgmt_no", nullable = false, length = 20)
  private String vehicleMgmtNo;

  @Column(name = "vehicle_no", length = 30)
  private String vehicleNo;

  @Column(name = "car_model", length = 100)
  private String carModel;

  @Column(name = "chassis_no", length = 50)
  private String chassisNo;

  @Column(name = "sale_date", nullable = false)
  private LocalDate saleDate;

  @Column(name = "buyer", nullable = false, length = 100)
  private String buyer;

  @Column(name = "sale_amount", nullable = false)
  private Long saleAmount;

  @Column(name = "supply_amount", nullable = false)
  private Long supplyAmount;

  @Column(name = "tax_amount", nullable = false)
  private Long taxAmount;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  /** BUG-04: 매각 등록 시 생성된 전표 ID. 수정 시 전표 재생성에 사용. */
  @Column(name = "voucher_id")
  private Long voucherId;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}