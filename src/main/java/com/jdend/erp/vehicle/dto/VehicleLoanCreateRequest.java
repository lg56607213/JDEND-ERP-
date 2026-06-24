package com.jdend.erp.vehicle.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleLoanCreateRequest {

  public String vehicleNo;
  public String loanType;

  public Long loanPrincipal;
  public Double loanInterest;
  public String financeName;

  public String repaymentMethod;
  public Integer repaymentPeriod;

  public Long downPayment;
  public Long deposit;

  public LocalDate startDate;
  public LocalDate endDate;

  public Integer paymentDay;
  public String monthlyPayment;

  public String repaymentAccount;
  
}
