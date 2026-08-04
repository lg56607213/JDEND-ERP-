package com.jdend.erp.payment.taxinvoice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class TaxInvoiceUpdateRequest {
    private LocalDate taxInvoiceDate; // 발행일자
    private Long supplyAmount;        // 공급가액
    private Long taxAmount;           // 세액
}
