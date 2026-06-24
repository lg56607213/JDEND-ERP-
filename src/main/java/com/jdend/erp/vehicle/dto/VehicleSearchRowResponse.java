package com.jdend.erp.vehicle.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleSearchRowResponse {
    public String vehicleMgmtNo;
    public String vehicleNo;
    public String carModel;
    public String makerContractNo;
}