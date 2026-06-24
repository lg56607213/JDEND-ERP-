package com.jdend.erp.vehicle.maintenance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "vehicle_maintenances")
public class VehicleMaintenance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "vehicle_order_id", nullable = false)
  private Long vehicleOrderId;

  @Column(name = "vehicle_mgmt_no", nullable = false)
  private String vehicleMgmtNo;

  @Column(name = "vehicle_no")
  private String vehicleNo;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "maintenance", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<VehicleMaintenanceItem> items = new ArrayList<>();

  public void addItem(VehicleMaintenanceItem item) {
    items.add(item);
    item.setMaintenance(this);
  }
}