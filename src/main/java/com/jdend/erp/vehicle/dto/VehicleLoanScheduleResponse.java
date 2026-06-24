package com.jdend.erp.vehicle.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleLoanScheduleResponse {
    private String vehicleNo;
    private String vehicleMgmtNo;

    private String financeName;
    private Long loanPrincipal;
    private Double loanInterest;
    private Integer repaymentPeriod;
    private String monthlyPayment;
    private Integer paymentDay;
    private String repaymentAccount;

    private List<VehicleLoanScheduleRowDto> originalSchedule;
    private List<VehicleLoanScheduleRowDto> adjustedSchedule;
}