package com.jdend.erp.loan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 고객이 홈페이지/링크로 넣는 대출(할부·리스) 신청.
 *
 * <p>고객은 로그인하지 않으므로 세션으로 회사 DB를 알 수 없다. 그래서 이 테이블은
 * <b>auth DB</b>에 두고, 신청 링크에 담긴 회사코드(login_users.login_id)로 어느 업체의
 * 신청인지 구분한다.</p>
 */
@Entity
@Table(name = "loan_applications",
       indexes = {
           @Index(name = "idx_loan_app_company", columnList = "company_code"),
           @Index(name = "idx_loan_app_status", columnList = "status")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class LoanApplication {

    /** 신청 방식 */
    public static final String TYPE_FORM  = "FORM";   // 조건까지 입력한 정식 신청
    public static final String TYPE_PHONE = "PHONE";  // 유선 문의 요청

    /** 처리 상태 */
    public static final String STATUS_NEW       = "NEW";        // 접수
    public static final String STATUS_CONTACTED = "CONTACTED";  // 상담중
    public static final String STATUS_DONE      = "DONE";       // 완료
    public static final String STATUS_CANCELED  = "CANCELED";   // 취소

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어느 업체로 들어온 신청인지 (login_users.login_id) */
    @Column(name = "company_code", nullable = false, length = 50)
    private String companyCode;

    @Column(name = "inquiry_type", nullable = false, length = 10)
    @Builder.Default
    private String inquiryType = TYPE_FORM;

    // ── 신청자 ──────────────────────────────────────────────

    @Column(name = "applicant_name", length = 50)
    private String applicantName;

    /** 연락받을 번호 (유선 문의는 이 값만으로도 접수된다) */
    @Column(name = "contact_phone", nullable = false, length = 30)
    private String contactPhone;

    // ── 신청 조건 (유선 문의는 비어 있을 수 있다) ─────────────

    /** 차종 — 목록이 아니라 직접 입력 */
    @Column(name = "car_model", length = 100)
    private String carModel;

    /** 출고 예상 시기 */
    @Column(name = "expected_delivery", length = 50)
    private String expectedDelivery;

    /** 할부 | 리스 */
    @Column(name = "finance_type", length = 10)
    private String financeType;

    /** 희망 선납금 비율 (10, 20, 30) */
    @Column(name = "down_payment_percent")
    private Integer downPaymentPercent;

    /** 희망 신청 기간 (36, 48, 60) */
    @Column(name = "term_months")
    private Integer termMonths;

    @Column(name = "memo", length = 500)
    private String memo;

    // ── 처리 ────────────────────────────────────────────────

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_NEW;

    @Column(name = "admin_memo", length = 500)
    private String adminMemo;

    /** 접수 경로 추적용 */
    @Column(name = "submit_ip", length = 60)
    private String submitIp;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
