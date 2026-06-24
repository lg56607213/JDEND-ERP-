package com.jdend.erp.contract.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ContractResponse {

  public Long id;
  public String contractNumber;

  public String customerNumber;
  public String customerName;

  public String vehicleNo;
  public String vehicleModel;

  public String contractType;
  public String contractCategory;
  public String status;

  public LocalDate startDate;
  public LocalDate endDate;

  public Integer billingCount;
  public Long monthlyRent;
}