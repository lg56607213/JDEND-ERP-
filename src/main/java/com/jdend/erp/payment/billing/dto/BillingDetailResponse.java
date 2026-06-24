package com.jdend.erp.payment.billing.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BillingDetailResponse {
  private Long id;
  private String billingNo;

  private String contractNumber;
  private String customerNumber;
  private String customerName;
  private String email;

  private LocalDate billingDate;
  private LocalDate taxStartDate;
  private LocalDate taxEndDate;

  private String status;
  private Long totalAmount;

  private List<BillingLineDto> lines;

  @Getter @Setter
  @NoArgsConstructor @AllArgsConstructor @Builder
  public static class BillingLineDto {
    private Integer installmentNo;
    private LocalDate taxInvoiceDate;
    private LocalDate dueDate;
    private Long rentAmount;
    private String contractNumber;
  }
}