package com.jdend.erp.payment.kiwoom.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 키움페이 LINK V3.1 결제 내역.
 * 멀티테넌시: 각 회사 DB에 생성됨(DynamicRoutingDataSource 경유).
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "kiwoom_payments")
public class KiwoomPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 우리가 생성한 주문번호 (KW + yyyyMMdd + 계약번호뒤4 + 3자리랜덤). UNIQUE */
    @Column(name = "order_id", nullable = false, unique = true, length = 50)
    private String orderId;

    /** 어떤 계약의 결제인지 */
    @Column(name = "contract_number", length = 30)
    private String contractNumber;

    /** 결제금액 (VAT 포함) */
    @Column(name = "amount")
    private Long amount;

    /** 결제수단 (CARD 등) */
    @Column(name = "paymethod", length = 20)
    private String paymethod;

    /** 키움페이 거래번호. 중복 통지 체크용. UNIQUE (null 허용: 결제 완료 전) */
    @Column(name = "daoutrx", unique = true, length = 20)
    private String daoutrx;

    /** 카드 승인번호 */
    @Column(name = "authno", length = 20)
    private String authno;

    /** 카드사 코드 */
    @Column(name = "card_code", length = 10)
    private String cardCode;

    /** 카드사 이름 */
    @Column(name = "card_name", length = 20)
    private String cardName;

    /** 카드번호 (마스킹) */
    @Column(name = "card_no", length = 20)
    private String cardNo;

    /** 할부개월 */
    @Column(name = "install_month", length = 5)
    private String installMonth;

    /** 결제 상태: PENDING / SUCCESS / FAIL / CANCEL */
    @Column(name = "status", length = 20)
    private String status;

    /** 결제일자 (키움페이 응답값) */
    @Column(name = "settdate", length = 20)
    private String settdate;

    /** 키움페이 통지 원문 (JSON) */
    @Column(name = "raw_notify_data", columnDefinition = "TEXT")
    private String rawNotifyData;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
