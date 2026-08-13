package com.jdend.erp.accounting.corporatecard.dto;

import lombok.Builder;
import lombok.Getter;

/** 조회기간 내 계정분류별 집계 (어느 계정으로 얼마가 잡혔는지). */
@Getter
@Builder
public class CorporateCardSummaryResponse {
    private String accountCode;
    private String accountName;
    private long count;
    private long totalAmount;
}
