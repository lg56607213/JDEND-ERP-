package com.jdend.erp.contract.sign.dto;

import lombok.*;

import java.time.LocalDateTime;

public class ContractSignDtos {

    /** 서명요청 생성 결과 (직원 화면) */
    @Getter @Builder
    public static class CreateResponse {
        private String token;
        private String signUrl;
        private String qrUrl;
        private LocalDateTime expiresAt;
    }

    /** 서명 페이지가 보여줄 계약 요약 (고객 화면) */
    @Getter @Builder
    public static class ContractSummary {
        private String contractNumber;
        private String customerName;
        private String vehicleNo;
        private String vehicleModel;
        private String contractType;
        private String startDate;
        private String endDate;
        private Long monthlyRent;
        private Long deposit;
        private Long advancePayment;
        private Integer billingCount;
        private String lessorName;
        private LocalDateTime expiresAt;
        private String status;
    }

    /** 고객이 제출하는 서명 */
    @Getter @Setter
    public static class SubmitRequest {
        private String signerName;
        private boolean agreed;
        /** data:image/png;base64,... 형식 */
        private String signatureImage;
    }

    @Getter @Builder
    public static class SubmitResponse {
        private String contractNumber;
        private LocalDateTime signedAt;
        private String documentHash;
    }

    /** 계약 화면에서 보여줄 서명 상태 */
    @Getter @Builder
    public static class StatusResponse {
        private String status;
        private LocalDateTime expiresAt;
        private LocalDateTime signedAt;
        private String signerName;
        private Long documentId;
    }
}
