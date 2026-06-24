package com.jdend.erp.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name="vehicle_order_history")
public class VehicleOrderHistory {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="vehicle_order_id", nullable=false)
  private VehicleOrder vehicleOrder;

  @Column(nullable=false, length=20)
  private String status;

  @Column(name="changed_at", nullable=false)
  private LocalDateTime changedAt;

  @Column(length=255)
  private String note;

  @PrePersist
  public void prePersist() {
    if (changedAt == null) changedAt = LocalDateTime.now();
  }
}
