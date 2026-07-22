package com.jdend.erp.legal.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LegalCostItemResponse {
    private Long id;
    private Long legalCaseId;
    private String costType;
    private Long amount;
    private LocalDate costDate;
    private String memo;
    private LocalDateTime createdAt;
}
