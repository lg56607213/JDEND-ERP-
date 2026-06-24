package com.jdend.erp.accounting.voucher.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleLookupRowResponse {
    private String vehicleNo;
    private String carModel;
}