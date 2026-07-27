package com.jdend.erp.accounting.prepaidrent.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class PrepaidRentHistoryResponse {

    private Long id;
    private LocalDate transactionDate;

    /** "입금" | "적용" */
    private String transactionType;

    private Long amount;

    /** 해당 행까지의 누적잔액 (입금 +, 적용 -) */
    private Long cumulativeBalance;

    private String memo;
    private Boolean voucherCreated;
}
