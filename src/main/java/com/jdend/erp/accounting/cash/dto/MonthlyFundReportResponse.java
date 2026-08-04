package com.jdend.erp.accounting.cash.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyFundReportResponse {

  @Getter @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class MonthBankRow {
    private String bankName;
    private Long prevBalance;   // 전월잔액 (해당 월 1일 이전 누적)
    private Long income;        // 월 수입 합계
    private Long expense;       // 월 지출 합계
    private Long balance;       // 잔액 = prevBalance + income - expense
  }

  @Getter @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class VoucherRow {
    private String accountName;
    private Long amount;
  }

  /** III. 월별 자금 현황 (통장별) */
  private List<MonthBankRow> banks;
  private Long totalPrevBalance;
  private Long totalIncome;
  private Long totalExpense;
  private Long totalBalance;

  /** IV. 월별 세부내역 — 현금성 계정(은행/예금 키워드) */
  private List<VoucherRow> voucherIncomes;
  private Long voucherIncomeTotal;
  private List<VoucherRow> voucherExpenses;
  private Long voucherExpenseTotal;

  /** IV. 월별 세부내역 — 해당 월 승인된 모든 전표 계정(현금성 필터 없음) */
  private List<VoucherRow> allVoucherDebits;    // 차변(DEBIT) 전체
  private Long allVoucherDebitTotal;
  private List<VoucherRow> allVoucherCredits;   // 대변(CREDIT) 전체
  private Long allVoucherCreditTotal;
}
