package com.jdend.erp.payment.taxinvoice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class TaxInvoiceDownloadSelectedRequest {
    private List<Long> scheduleIds;
}
