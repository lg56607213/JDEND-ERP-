package com.jdend.erp.vehicle.maintenance.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleMaintenanceItemUpdateRequest {

  private LocalDate maintenanceDate;
  private LocalDate payDate;
  private String description;
  private Long amount;
  private Long supplyAmount;
  private Long vatAmount;
  private String vendor;
  private String paymentMethod;
}