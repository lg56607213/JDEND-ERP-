package com.jdend.erp.accounting.statements.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatementNodeResponse {
  private String accountCode;
  private String accountName;
  private Integer level;
  private Long amount;
  private List<StatementNodeResponse> children;
}
