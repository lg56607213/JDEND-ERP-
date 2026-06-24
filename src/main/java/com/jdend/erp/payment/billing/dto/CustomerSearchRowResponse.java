package com.jdend.erp.payment.billing.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerSearchRowResponse {
  private String customerNumber;
  private String customerName;
  private String email;
}