package com.jdend.erp.accounting.cash.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyFundReportResponse {

  @Getter @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class BankRow {
    private String bankName;
    private Long income;
    private Long expense;
  }

  @Getter @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class VoucherRow {
    private String accountCode;   // 테이블에 없으니 일단 "" 로 내려줌
    private String accountName;
    private Long amount;
    private String memo;          // 일단 "" 로 내려줌(전표라인에 메모 없음)
  }

  private List<BankRow> banks;
  private Long bankIncomeTotal;
  private Long bankExpenseTotal;

  private List<VoucherRow> voucherIncomes;
  private Long voucherIncomeTotal;

  private List<VoucherRow> voucherExpenses;
  private Long voucherExpenseTotal;

  private Long incomeDiff;   // bankIncomeTotal - voucherIncomeTotal
  private Long expenseDiff;  // bankExpenseTotal - voucherExpenseTotal
}