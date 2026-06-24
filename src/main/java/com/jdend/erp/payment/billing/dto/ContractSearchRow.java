package com.jdend.erp.payment.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ContractSearchRow {
  private String contractNumber;
  private String customerNumber;
}