package com.jdend.erp.vehicle.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanVehiclePickerRowDto {
    private Long loanId;
    private String vehicleMgmtNo;
    private String vehicleNo;
    private String carModel;
    private String financeName;
}