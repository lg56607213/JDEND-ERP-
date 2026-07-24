package com.jdend.erp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "kiwoompay")
public class KiwoomPayProperties {

    /** 키움페이 상점 ID (MID). 테스트: CTS0000000 */
    private String cpid = "CTS0000000";

    /** 1단계 해시 요청 URL */
    private String hashUrl = "https://apitest.kiwoompay.co.kr/pay/hash";

    /** 2단계 결제창 POST URL */
    private String payUrl = "https://apitest.kiwoompay.co.kr/pay/linkEnc";

    /** 통지 수신 허용 IP 목록 (키움페이 서버 IP). 빈 목록이면 IP 체크 생략(개발 전용). */
    private List<String> notifyAllowIps;

    /** 취소(환불) API URL (1단계 Ready). 테스트: https://apitest.kiwoompay.co.kr/pay/ready */
    private String cancelUrl = "https://apitest.kiwoompay.co.kr/pay/ready";

    /** 키움페이 상점관리자에서 설정하는 8자리 암호화 키 (Authorization 헤더). 취소 API용. */
    private String authorization = "";
}
