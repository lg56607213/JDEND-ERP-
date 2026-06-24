package com.jdend.erp.payment.banktx.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BankAccountPickRowResponse {
  private String bankName;
  private String accountNo;
}