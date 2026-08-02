package com.jdend.erp.legal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "legal_progress_entries")
public class LegalProgressEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legal_case_id", nullable = false)
    private Long legalCaseId;

    @Column(name = "progress_date", nullable = false)
    private LocalDate progressDate;

    @Column(name = "progress_content", columnDefinition = "TEXT", nullable = false)
    private String progressContent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
