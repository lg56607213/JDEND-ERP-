package com.jdend.erp.payment.kiwoom.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 백엔드 → 프론트엔드: 결제창 진입에 필요한 정보.
 * 프론트는 payUrl 로 form POST 하면서 params 를 hidden input 으로 첨부한다.
 */
@Getter
@Builder
public class KiwoomPayReadyResponse {

    /** 결제창 POST 대상 URL (kiwoompay.pay-url) */
    private String payUrl;

    /**
     * 결제 파라미터 맵.
     * 키: PAYMETHOD / TYPE / CPID / ORDERNO / AMOUNT / PRODUCTTYPE / PRODUCTNAME /
     *     PRODUCTCODE / TAXFREECD / KIWOOM_ENC / HOMEURL / FAILURL / CLOSEURL /
     *     DIRECTRESULTFLAG / RESERVEDINDEX1
     */
    private Map<String, String> params;
}
