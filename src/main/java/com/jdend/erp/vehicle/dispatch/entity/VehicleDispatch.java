package com.jdend.erp.vehicle.dispatch.entity;

import com.jdend.erp.vehicle.entity.VehicleOrder;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "vehicle_dispatches")
public class VehicleDispatch {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_order_id")
  private VehicleOrder vehicleOrder;

  @Column(name = "vehicle_mgmt_no", nullable = false, length = 20)
  private String vehicleMgmtNo;

  @Column(name = "vehicle_no", length = 30)
  private String vehicleNo;

  // ✅ DB에 추가한 컬럼
  @Column(name = "contract_number", length = 50)
  private String contractNumber;

  @Column(name = "dispatch_type", nullable = false, length = 20)
  private String dispatchType;

  @Column(name = "dispatch_date", nullable = false)
  private LocalDate dispatchDate;

  @Column(name = "departure_address", nullable = false, length = 255)
  private String departureAddress;

  @Column(name = "departure_contact", nullable = false, length = 30)
  private String departureContact;

  @Column(name = "arrival_address", nullable = false, length = 255)
  private String arrivalAddress;

  @Column(name = "arrival_contact", nullable = false, length = 30)
  private String arrivalContact;

  @Column(name = "remarks", length = 500)
  private String remarks;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}