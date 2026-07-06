package com.jdend.erp.consultation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tax_consultations")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxConsultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_name", length = 200)
    private String fileName;

    @Column(name = "file_original_name", length = 200)
    private String fileOriginalName;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "answered_by", length = 100)
    private String answeredBy;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING / ANSWERED

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
