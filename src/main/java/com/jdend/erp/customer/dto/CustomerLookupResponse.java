package com.jdend.erp.customer.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CustomerLookupResponse {
  private String customerNumber;
  private String customerName;
  private String phone;
  private String address;
  private String registrationNumber;
}