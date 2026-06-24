package com.jdend.erp.contract.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NextContractNumberResponse {
  private String contractNumber;
}