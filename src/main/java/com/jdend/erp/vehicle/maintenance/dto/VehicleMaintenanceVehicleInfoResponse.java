package com.jdend.erp.vehicle.maintenance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class VehicleMaintenanceVehicleInfoResponse {
  private String vehicleMgmtNo;
  private String vehicleNo;
  private LocalDate inspectionStart;
  private LocalDate inspectionEnd;
}