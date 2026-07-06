package com.jdend.erp.accounting.payable.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class PayableLineResponse {
    private Long id;
    private String voucherNo;
    private LocalDate voucherDate;
    private String accountName;
    private Long amount;
    private String description;
    private String memo;
    private String contractNumber;
}
