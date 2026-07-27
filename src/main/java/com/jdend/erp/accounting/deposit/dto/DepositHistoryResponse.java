package com.jdend.erp.accounting.deposit.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class DepositHistoryResponse {

    private Long id;
    private LocalDate transactionDate;

    /** "수납" | "지급" */
    private String transactionType;

    private Long amount;

    /** 해당 행까지의 누적잔액 (수납은 +, 지급은 -) */
    private Long cumulativeBalance;

    private String memo;
    private Boolean voucherCreated;
}
