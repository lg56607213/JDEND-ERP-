package com.jdend.erp.vehicle.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleLoanRepaymentStatusRowResponse {

    private Integer installmentNo;
    private LocalDate paymentDate;

    private Long monthlyPayment;
    private Long principalAmount;
    private Long interestAmount;
    private Long remainingPrincipal;

    private Long paidAmount;
    private Long unpaidAmount;
    private Long receivableAmount;

    private String repaymentAccount;
    private String status;
}