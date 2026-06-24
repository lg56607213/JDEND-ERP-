package com.jdend.erp.vehicle.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleLoanListItemResponse {
  public Long id;

  public String vehicleMgmtNo;
  public String makerContractNo; // vehicle_orders.maker_contract_no
  public String vehicleNo;       // vehicle_orders.vehicle_no

  public String loanType;
  public Long loanPrincipal;
  public Double loanInterest;
  public String financeName;

  public String repaymentMethod;
  public Integer repaymentPeriod;

  public Integer paymentDay;
  public String monthlyPayment;

  public String repaymentAccount;     // ✅ 상환계좌
  public Long remainingPrincipal;     // ✅ 미회수원금
  public LocalDate lastPaymentDate;   // ✅ 최근차입금상환일

  public Boolean terminated;
}
