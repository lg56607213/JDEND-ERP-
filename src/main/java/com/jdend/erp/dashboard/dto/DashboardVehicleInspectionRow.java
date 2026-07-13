package com.jdend.erp.dashboard.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DashboardVehicleInspectionRow {
    private String vehicleMgmtNo;
    private String vehicleNo;
    private String carModel;
    private LocalDate inspectionEndDate;
    /** 정상 / 만료임박 / 만료 / 미등록 */
    private String status;
    private Long dday;
}
