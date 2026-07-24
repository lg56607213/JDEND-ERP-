package com.jdend.erp.subscription.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * jdend.co.kr 결제 신청 버튼 → POST /api/subscription/kiwoom/ready 요청 바디.
 */
@Getter
@Setter
public class SubscriptionPayReadyRequest {

    /** 신청자명 */
    private String applicantName;

    /** 회사명 */
    private String companyName;

    /** 연락 이메일 */
    private String email;

    /** 연락처 */
    private String phone;

    /** 요금제명 (Basic/Pro/Max) */
    private String planName;

    /** 결제금액 (원 단위 정수) */
    private Long amount;

    /** P = PC, M = 모바일 (프론트에서 userAgent로 감지) */
    private String type;
}
