package com.jdend.erp.vehicle.inspection.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class VehicleInspectionCreateRequest {
  public String vehicleMgmtNo;
  public LocalDate inspectionDate;
  public LocalDate validStart;
  public LocalDate validEnd;
  public String vendor;
  public String inspectionPlace;
  public Long inspectionCost;
  public String memo;
}