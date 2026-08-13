package com.jdend.erp.accounting.corporatecard.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class CorporateCardRowResponse {
    private Long id;
    private LocalDate useDate;
    private String cardName;
    private String summary;
    private Long amount;
    private String detail;
    private String accountCode;
    private String accountName;
}
