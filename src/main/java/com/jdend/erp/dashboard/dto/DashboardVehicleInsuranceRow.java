package com.jdend.erp.dashboard.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DashboardVehicleInsuranceRow {
    private String vehicleMgmtNo;
    private String vehicleNo;
    private String carModel;
    private LocalDate insuranceEndDate;
    /** 정상 / 만료임박 / 만료 / 미가입 */
    private String status;
    private Long dday;
}
