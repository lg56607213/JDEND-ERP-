package com.jdend.erp.subscription.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * POST /api/subscription/kiwoom/ready 응답.
 * 프론트는 payUrl 에 params 를 form POST 해서 키움페이 결제창을 열어야 한다.
 */
@Getter
@Builder
public class SubscriptionPayReadyResponse {

    /** 키움페이 결제창 POST URL */
    private String payUrl;

    /** 결제창 진입에 필요한 폼 파라미터 전체 */
    private Map<String, String> params;

    /** 우리가 생성한 주문번호 (결제 결과 조회용) */
    private String orderId;
}
