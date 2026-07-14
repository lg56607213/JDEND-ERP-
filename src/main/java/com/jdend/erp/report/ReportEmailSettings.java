package com.jdend.erp.report;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_email_settings")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportEmailSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false, unique = true)
    private Long companyId;

    // 수신자 이메일 목록 — 쉼표로 구분 (예: "a@b.com,c@d.com")
    @Column(name = "recipient_emails", columnDefinition = "TEXT")
    private String recipientEmails;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Column(name = "created_at",
            columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)",
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at",
            columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)",
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
