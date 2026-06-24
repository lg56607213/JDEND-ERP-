package com.jdend.erp.contract.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ContractRequest {

  public String customerNumber; // 고객번호(C001)
  public String vehicleNo;
  public String vehicleModel;

  public String contractType;
  public String contractCategory;
  public String status;

  public LocalDate startDate;
  public LocalDate endDate;

  public Integer taxInvoiceDay;
  public String paymentDueDay;

  public Long advancePayment;
  public Long monthlyRent;
  public Integer billingDay;
  public Integer billingCount;
  public Long totalRent;

  public Long deposit;
  public String maturityOption;
  public Long residualValue;

  public String vehicleInsurance;
  public String insuranceAge;
  public String vehicleInsuranceLimit;
  public String vehicleDeductible;

  public String propertyLiability;
  public String propertyDeductible;
  public String personalDeductible;
  public String passengerDeductible;

  public String remarks;
}