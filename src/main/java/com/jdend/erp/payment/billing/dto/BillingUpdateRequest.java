package com.jdend.erp.payment.billing.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BillingUpdateRequest {
  private String customerName;
  private String email;
  private Long totalAmount;
}