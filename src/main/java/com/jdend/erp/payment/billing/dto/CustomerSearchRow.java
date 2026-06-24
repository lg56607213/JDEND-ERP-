package com.jdend.erp.payment.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomerSearchRow {
  private String customerNumber;
  private String customerName;
  private String email;
}