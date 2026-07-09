package com.jdend.erp.payment.taxinvoice.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TaxInvoicePreviewRow {
    private String contractNumber;
    private Integer installmentNo;
    private String vehicleNo;
    private String vehicleModel;
    private String customerName;
    private String ceo;
    private String registrationNumber;
    private String address;
    private String businessType;
    private String businessItem;
    private String email;
    private LocalDate taxInvoiceDate;
    private Long supplyAmount;
    private Long taxAmount;
}
