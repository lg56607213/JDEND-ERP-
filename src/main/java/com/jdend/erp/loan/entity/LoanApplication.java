package com.jdend.erp.loan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 영업용 차량 증차 자금 신청.
 *
 * <p>ERP를 쓰는 렌터카 업체가 차량을 늘릴 때 신청하고, 운영자(플랫폼)가 접수해
 * 제휴 금융사에 알선한다. 신청 주체가 업체 자신이므로 로그인 세션에서 업체를 확정한다.</p>
 *
 * <p>업체 정보는 auth DB(login_users)에 있고 신청은 업체 구분 없이 운영자가 한 번에
 * 봐야 하므로, 이 테이블도 <b>auth DB</b>에 둔다.</p>
 */
@Entity
// 고객용으로 먼저 만들었던 loan_applications 와 컬럼 구성이 달라 테이블을 분리한다.
@Table(name = "vehicle_loan_applications",
       indexes = {
           @Index(name = "idx_vla_company", columnList = "company_id"),
           @Index(name = "idx_vla_status", columnList = "status")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class LoanApplication {

    /** 신청 방식 */
    public static final String TYPE_FORM  = "FORM";   // 조건까지 입력한 정식 신청
    public static final String TYPE_PHONE = "PHONE";  // 유선 상담 요청

    /** 처리 상태 */
    public static final String STATUS_NEW       = "NEW";        // 접수
    public static final String STATUS_REVIEWING = "REVIEWING";  // 심사/알선중
    public static final String STATUS_DONE      = "DONE";       // 실행완료
    public static final String STATUS_REJECTED  = "REJECTED";   // 부결
    public static final String STATUS_CANCELED  = "CANCELED";   // 취소

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신청 업체 (login_users.id) */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /** 접수 시점의 업체명 — 운영자 목록에서 바로 보기 위한 사본 */
    @Column(name = "company_name", length = 100)
    private String companyName;

    /** 실제로 신청 버튼을 누른 ERP 사용자 아이디 */
    @Column(name = "requested_by", length = 50)
    private String requestedBy;

    @Column(name = "inquiry_type", nullable = false, length = 10)
    @Builder.Default
    private String inquiryType = TYPE_FORM;

    // ── 연락 ────────────────────────────────────────────────

    @Column(name = "manager_name", length = 50)
    private String managerName;

    /** 연락받을 번호 */
    @Column(name = "contact_phone", nullable = false, length = 30)
    private String contactPhone;

    // ── 신청 조건 (유선 상담 요청은 비어 있을 수 있다) ────────

    /** 증차하려는 차종 — 목록이 아니라 직접 입력 */
    @Column(name = "car_model", length = 100)
    private String carModel;

    /** 증차 대수 */
    @Column(name = "vehicle_count")
    private Integer vehicleCount;

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

    /** 운영자 메모 (신청 업체에는 보이지 않는다) */
    @Column(name = "admin_memo", length = 500)
    private String adminMemo;

    /** 업체에게 안내하는 진행 내용 */
    @Column(name = "reply_message", length = 500)
    private String replyMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
