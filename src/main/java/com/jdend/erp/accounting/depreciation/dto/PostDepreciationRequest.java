package com.jdend.erp.accounting.depreciation.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDepreciationRequest {
  public String baseMonth;      // YYYY-MM
  public LocalDate voucherDate;
  public List<Long> assetIds;
}