package com.jdend.erp.dashboard.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardBankDiffRow {
    private LocalDate txDate;
    private Long bankDeposit;
    private Long bankWithdrawal;
    private Long voucherDeposit;    // 전표 DEBIT 보통예금
    private Long voucherWithdrawal; // 전표 CREDIT 보통예금
    private Long depositDiff;       // 은행입금 - 전표입금
    private Long withdrawalDiff;    // 은행출금 - 전표출금
}
