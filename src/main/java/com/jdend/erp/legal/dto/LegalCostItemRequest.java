package com.jdend.erp.legal.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LegalCostItemRequest {
    /** 신청비용 / 추가비용 / 확인비용 / 환입 */
    private String costType;
    private Long amount;
    private LocalDate costDate;
    private String memo;
}
