package com.jdend.erp.payment.banktx.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BankTransactionRowResponse {
  private Long id;
  private String bankName;
  private String accountNo;
  private LocalDate txDate;
  private Long depositAmount;
  private Long withdrawalAmount;
  private String summary;
  private String remarks;
}