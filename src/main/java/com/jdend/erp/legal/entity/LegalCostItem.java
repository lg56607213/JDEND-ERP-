package com.jdend.erp.legal.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "legal_cost_items")
public class LegalCostItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legal_case_id", nullable = false)
    private Long legalCaseId;

    /** 신청비용 / 추가비용 / 확인비용 / 환입 */
    @Column(name = "cost_type", nullable = false, length = 20)
    private String costType;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "cost_date", nullable = false)
    private LocalDate costDate;

    @Column(name = "memo", length = 200)
    private String memo;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
