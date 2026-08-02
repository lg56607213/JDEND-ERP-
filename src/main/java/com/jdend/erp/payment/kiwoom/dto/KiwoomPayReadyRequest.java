package com.jdend.erp.payment.kiwoom.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 프론트엔드 → 백엔드: 결제 준비 요청.
 */
@Getter
@Setter
public class KiwoomPayReadyRequest {

    /** 계약번호 */
    private String contractNumber;

    /** 결제금액 (VAT 포함, 원 단위 정수) */
    private Long amount;

    /** 상품명 (키움페이 화면에 표시됨). | 포함 불가 — 서비스에서 제거 처리. */
    private String productName;
}
