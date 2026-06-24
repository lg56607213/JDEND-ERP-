// src/main/java/com/jdend/erp/contract/dto/ContractStatusRowResponse.java
package com.jdend.erp.contract.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractStatusRowResponse {

  private String vehicleNumber;
  private String contractNumber;
  private String status;
  private String customerName;

  private LocalDate contractStart;
  private LocalDate contractEnd;

  private Long monthlyRent;
  private Long totalRent;

  private Long receivable;
  private Long advance;

  private String vehicleType;
}