package com.jdend.erp.contract.sign.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 계약서 전자서명 요청.
 *
 * <p>서명 링크는 로그인 없이 열리므로 세션으로 회사 DB를 알 수 없다.
 * 따라서 이 테이블은 <b>auth DB</b>에 두고 tenantDb 컬럼으로 어느 회사의
 * 계약인지를 함께 보관한다.</p>
 *
 * <p>토큰 원문은 저장하지 않고 SHA-256 해시만 저장한다. DB가 유출돼도
 * 그것만으로는 서명 링크를 만들 수 없다.</p>
 */
@Entity
@Table(name = "contract_sign_requests",
       indexes = @Index(name = "idx_csr_token_hash", columnList = "token_hash"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ContractSignRequest {

    public static final String STATUS_PENDING  = "PENDING";
    public static final String STATUS_SIGNED   = "SIGNED";
    public static final String STATUS_CANCELED = "CANCELED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** 계약이 들어있는 회사 DB명 (예: erp_company_jdend) */
    @Column(name = "tenant_db", nullable = false, length = 100)
    private String tenantDb;

    @Column(name = "contract_number", nullable = false, length = 50)
    private String contractNumber;

    /** 화면 표시용 (서명 페이지에서 누구 계약인지 보여주기 위함) */
    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_PENDING;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── 서명 증거 ────────────────────────────────────────────

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "signer_name", length = 100)
    private String signerName;

    @Column(name = "signer_ip", length = 60)
    private String signerIp;

    @Column(name = "signer_user_agent", length = 400)
    private String signerUserAgent;

    /** 서명 대상 문서(PDF)의 SHA-256 — 이후 내용이 바뀌지 않았음을 증명한다 */
    @Column(name = "document_hash", length = 64)
    private String documentHash;

    /** 생성된 계약서 PDF의 vehicle_documents.id (회사 DB 기준) */
    @Column(name = "document_id")
    private Long documentId;

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
