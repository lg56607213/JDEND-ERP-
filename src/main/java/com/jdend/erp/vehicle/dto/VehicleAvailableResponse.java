package com.jdend.erp.vehicle.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleAvailableResponse {
  private String vehicleNo;
  private String vehicleModel;
  private String vehicleMgmtNo;
}