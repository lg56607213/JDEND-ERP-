package com.jdend.erp.legal.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LegalProgressResponse {
    private Long id;
    private Long legalCaseId;
    private LocalDate progressDate;
    private String progressContent;
    private LocalDateTime createdAt;
}
