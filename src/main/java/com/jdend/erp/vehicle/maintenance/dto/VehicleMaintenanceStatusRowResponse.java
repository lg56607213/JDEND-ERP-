package com.jdend.erp.vehicle.maintenance.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleMaintenanceStatusRowResponse {

  private Long maintenanceId;
  private Long itemId;
  private String vehicleMgmtNo;
  private String vehicleNo;
  private LocalDate maintenanceDate;
  private String description;
  private Long amount;
  private Long supplyAmount;
  private Long vatAmount;
  private String vendor;
  private String paymentMethod;
  private String paymentDetail;
}