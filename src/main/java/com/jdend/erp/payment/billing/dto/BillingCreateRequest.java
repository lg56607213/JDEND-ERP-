package com.jdend.erp.payment.billing.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class BillingCreateRequest {
  private String type;            // batch | individual
  private LocalDate billingDate;
  private LocalDate taxStartDate;
  private LocalDate taxEndDate;
  private String customerNumber;  // 개별생성일 때만
  private String overdueType;     // all | exclude | only
  private String contractCategory; // 장기 | 단기 | null(전체)
}