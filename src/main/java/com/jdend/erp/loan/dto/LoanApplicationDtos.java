package com.jdend.erp.loan.dto;

import com.jdend.erp.loan.entity.LoanApplication;
import lombok.*;

import java.time.LocalDateTime;

public class LoanApplicationDtos {

    /** 고객이 공개 페이지에서 보내는 신청 */
    @Getter @Setter
    public static class SubmitRequest {
        /** 회사코드 (신청 링크의 c 파라미터) */
        private String companyCode;
        /** FORM | PHONE */
        private String inquiryType;
        private String applicantName;
        private String contactPhone;
        private String carModel;
        private String expectedDelivery;
        private String financeType;
        private Integer downPaymentPercent;
        private Integer termMonths;
        private String memo;
        /** 개인정보 수집·이용 동의 */
        private boolean agreed;
    }

    @Getter @Builder
    public static class SubmitResponse {
        private Long id;
        private String message;
    }

    /** 공개 페이지가 표시할 업체 정보 */
    @Getter @Builder
    public static class PublicCompanyResponse {
        private String companyCode;
        private String companyName;
    }

    /** ERP 목록 행 */
    @Getter @Builder
    public static class RowResponse {
        private Long id;
        private String inquiryType;
        private String inquiryTypeLabel;
        private String applicantName;
        private String contactPhone;
        private String carModel;
        private String expectedDelivery;
        private String financeType;
        private Integer downPaymentPercent;
        private Integer termMonths;
        private String memo;
        private String status;
        private String adminMemo;
        private LocalDateTime createdAt;

        public static RowResponse from(LoanApplication a) {
            return RowResponse.builder()
                    .id(a.getId())
                    .inquiryType(a.getInquiryType())
                    .inquiryTypeLabel(LoanApplication.TYPE_PHONE.equals(a.getInquiryType())
                            ? "유선문의" : "신청서")
                    .applicantName(a.getApplicantName())
                    .contactPhone(a.getContactPhone())
                    .carModel(a.getCarModel())
                    .expectedDelivery(a.getExpectedDelivery())
                    .financeType(a.getFinanceType())
                    .downPaymentPercent(a.getDownPaymentPercent())
                    .termMonths(a.getTermMonths())
                    .memo(a.getMemo())
                    .status(a.getStatus())
                    .adminMemo(a.getAdminMemo())
                    .createdAt(a.getCreatedAt())
                    .build();
        }
    }

    /** 상태/메모 변경 */
    @Getter @Setter
    public static class UpdateRequest {
        private String status;
        private String adminMemo;
    }

    /** 목록 화면 상단 요약 */
    @Getter @Builder
    public static class SummaryResponse {
        private long newCount;
        private long contactedCount;
        private long doneCount;
        /** 고객에게 보낼 신청 링크 */
        private String applyUrl;
    }
}
