package com.jdend.erp.payment.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BillingCreateResponse {
  private String billingNo;
  private int createdLines;
  private int skippedLines;
  private long totalAmount;
}