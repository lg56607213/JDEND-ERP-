package com.jdend.erp.dashboard.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardBankSummaryRow {
    private String bankName;
    private String accountNumber;
    private String accountAlias;
    private Long balance2DaysAgo;   // 2일전 현금잔액
    private Long yesterdayDeposit;  // 어제 입금
    private Long yesterdayWithdrawal; // 어제 출금
    private Long currentBalance;    // 최종잔액
}
