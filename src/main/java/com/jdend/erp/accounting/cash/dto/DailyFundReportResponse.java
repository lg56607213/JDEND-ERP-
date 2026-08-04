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
    private Long prevBalance;  // 전일잔액
    private Long income;
    private Long expense;
    private Long balance;      // 금일잔액 = prevBalance + income - expense
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
  private Long bankPrevBalanceTotal;  // 전일잔액 합계
  private Long bankIncomeTotal;
  private Long bankExpenseTotal;
  private Long bankBalanceTotal;      // 금일잔액 합계

  /** II. 세부내역 — 현금성 계정(은행/예금 키워드) */
  private List<VoucherRow> voucherIncomes;
  private Long voucherIncomeTotal;

  private List<VoucherRow> voucherExpenses;
  private Long voucherExpenseTotal;

  private Long incomeDiff;   // bankIncomeTotal - voucherIncomeTotal
  private Long expenseDiff;  // bankExpenseTotal - voucherExpenseTotal

  /** II. 세부내역 — 그날 승인된 모든 전표 계정(현금성 필터 없음) */
  private List<VoucherRow> allVoucherDebits;    // 차변(DEBIT) 전체
  private Long allVoucherDebitTotal;
  private List<VoucherRow> allVoucherCredits;   // 대변(CREDIT) 전체
  private Long allVoucherCreditTotal;
}