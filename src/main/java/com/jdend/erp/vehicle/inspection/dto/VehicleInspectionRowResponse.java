package com.jdend.erp.vehicle.inspection.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleInspectionRowResponse {
  public Long id;
  public String vehicleMgmtNo;
  public String vehicleNo;
  public String carModel;
  public LocalDate validStart;
  public LocalDate validEnd;
  public LocalDate inspectionDate;
}