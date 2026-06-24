package com.jdend.erp.contract.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractUpdateRequest {

  private String customerNumber;
  private String vehicleNo;

  private String contractType;
  private String contractCategory;
  private LocalDate startDate;
  private LocalDate endDate;
  private Integer taxInvoiceDay;
  private String paymentDueDay;

  private Long advancePayment;
  private Long monthlyRent;
  private Integer billingDay;
  private Integer billingCount;
  private Long deposit;
  private String maturityOption;
  private Long residualValue;

  private String vehicleInsurance;
  private String insuranceAge;
  private String vehicleInsuranceLimit;
  private String vehicleDeductible;
  private String propertyLiability;
  private String propertyDeductible;
  private String personalDeductible;
  private String passengerDeductible;

  private String remarks;
}