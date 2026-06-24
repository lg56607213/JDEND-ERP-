package com.jdend.erp.vehicle.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleLoanScheduleRowDto {
    private Integer installmentNo;
    private LocalDate paymentDate;
    private Long monthlyPayment;
    private Long principalAmount;
    private Long interestAmount;
    private Long remainingPrincipal;
    private String repaymentAccount;
}