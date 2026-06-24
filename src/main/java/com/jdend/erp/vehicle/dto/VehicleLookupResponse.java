package com.jdend.erp.vehicle.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleLookupResponse {
  public String vehicleNo;
  public String vehicleMgmtNo;
  public String orderStatus;
  public String makerContractNo;
  public String carModel;
}
