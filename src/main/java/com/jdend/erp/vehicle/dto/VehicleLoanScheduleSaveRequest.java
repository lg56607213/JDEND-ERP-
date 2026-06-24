package com.jdend.erp.vehicle.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleLoanScheduleSaveRequest {
    private String vehicleNo;
    private List<VehicleLoanScheduleRowDto> adjustedSchedule;
}