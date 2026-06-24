package com.jdend.erp.vehicle.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleLoanUpdateRequest {
    public String financeName;
    public Long loanPrincipal;
    public Double loanInterest;
    public Integer repaymentPeriod;
    public Integer paymentDay;
    public String monthlyPayment;
    public String repaymentAccount;
    public Long remainingPrincipal;
}