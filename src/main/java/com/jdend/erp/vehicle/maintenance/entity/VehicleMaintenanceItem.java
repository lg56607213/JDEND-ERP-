package com.jdend.erp.vehicle.maintenance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vehicle_maintenance_items")
public class VehicleMaintenanceItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "maintenance_id", nullable = false)
  private VehicleMaintenance maintenance;

  @Column(name = "description", nullable = false)
  private String description;

  @Column(name = "amount", nullable = false)
  private Long amount;

  @Column(name = "supply_amount", nullable = false)
  private Long supplyAmount;

  @Column(name = "vat_amount", nullable = false)
  private Long vatAmount;

  @Column(name = "vendor")
  private String vendor;

  @Column(name = "pay_date")
  private LocalDate payDate;

  @Column(name = "payment_method", length = 50)
  private String paymentMethod;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}