package com.jdend.erp.vehicle.maintenance.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VehicleMaintenanceCreateRequest {
  private String vehicleMgmtNo;
  private List<VehicleMaintenanceItemRequest> items;
}