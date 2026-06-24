package com.jdend.erp.contract.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractDetailResponse {

  private Long id;
  private String contractNumber;

  private String customerNumber;
  private String customerName;
  private String customerPhone;
  private String customerAddress;
  private String customerRegistrationNumber;

  private String vehicleNo;
  private String vehicleModel;

  private String contractType;
  private String contractCategory;
  private String status;
  private LocalDate startDate;
  private LocalDate endDate;
  private Integer taxInvoiceDay;
  private String paymentDueDay;

  private Long advancePayment;
  private Long monthlyRent;
  private Integer billingDay;
  private Integer billingCount;
  private Long totalRent;
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