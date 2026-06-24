package com.jdend.erp.vehicle.inspection.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleInspectionResponse {
  public Long id;
  public Long vehicleOrderId;
  public String vehicleMgmtNo;
  public String vehicleNo;
  public LocalDate inspectionDate;
  public LocalDate validStart;
  public LocalDate validEnd;
  public String vendor;
  public String inspectionPlace;
  public Long inspectionCost;
  public String memo;
}