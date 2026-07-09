package com.jdend.erp.legal.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class LegalProgressRequest {
    private LocalDate progressDate;
    private String progressContent;
}
