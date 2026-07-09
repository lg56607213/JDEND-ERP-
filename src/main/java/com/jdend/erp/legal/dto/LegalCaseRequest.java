package com.jdend.erp.legal.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LegalCaseRequest {
    private String contractNumber;
    private String caseType;
    private String caseNumber;
    private LocalDate registrationDate;
    private Long legalCostPayment;
    private Long legalCostRefund;
    private String status;
}
