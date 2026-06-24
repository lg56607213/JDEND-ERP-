package com.jdend.erp.vehicle.inspection.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "vehicle_inspections")
public class VehicleInspection {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "vehicle_order_id")
  private Long vehicleOrderId;

  @Column(name = "vehicle_mgmt_no", nullable = false, length = 20)
  private String vehicleMgmtNo;

  @Column(name = "vehicle_no", length = 30)
  private String vehicleNo;

  @Column(name = "inspection_date")
  private LocalDate inspectionDate;

  @Column(name = "valid_start", nullable = false)
  private LocalDate validStart;

  @Column(name = "valid_end", nullable = false)
  private LocalDate validEnd;

  @Column(name = "vendor", length = 100)
  private String vendor;

  @Column(name = "inspection_place", length = 100)
  private String inspectionPlace;

  @Column(name = "inspection_cost")
  private Long inspectionCost;

  @Column(name = "memo", length = 255)
  private String memo;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}