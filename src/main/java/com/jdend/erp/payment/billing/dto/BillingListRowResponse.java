package com.jdend.erp.payment.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class BillingListRowResponse {
  private String billingNo;
  private String contractNumber;
  private String customerName;
  private String email;
  private LocalDate billingDate;
  private String status;
  private Long totalAmount;
}