package com.jdend.erp.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 구독 결제 내역.
 * jdend.co.kr 홈페이지에서 결제 신청 → auth DB에 저장 (멀티테넌시 공유 DB).
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "subscription_payments")
public class SubscriptionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신청자명 */
    @Column(name = "applicant_name", length = 50)
    private String applicantName;

    /** 회사명 */
    @Column(name = "company_name", length = 100)
    private String companyName;

    /** 연락 이메일 */
    @Column(name = "email", length = 100)
    private String email;

    /** 연락처 */
    @Column(name = "phone", length = 20)
    private String phone;

    /** 요금제명 (Basic/Pro/Max) */
    @Column(name = "plan_name", length = 50)
    private String planName;

    /** 결제금액 */
    @Column(name = "amount")
    private Long amount;

    /** 우리 주문번호 (SUB + YYYYMMDD + 5자리랜덤). UNIQUE */
    @Column(name = "order_id", unique = true, length = 50)
    private String orderId;

    /** 키움페이 거래번호. 중복 통지 체크용. UNIQUE (null 허용) */
    @Column(name = "daoutrx", unique = true, length = 20)
    private String daoutrx;

    /** 카드 승인번호 */
    @Column(name = "authno", length = 20)
    private String authno;

    /** 카드사명 */
    @Column(name = "card_name", length = 20)
    private String cardName;

    /** 결제 상태: PENDING / SUCCESS / FAIL / CANCEL */
    @Column(name = "status", length = 20)
    private String status;

    /** 관리자 계정 생성 완료 여부 */
    @Column(name = "account_created")
    @Builder.Default
    private Boolean accountCreated = false;

    /** 관리자 메모 */
    @Column(name = "memo", length = 500)
    private String memo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
