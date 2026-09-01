package com.jdend.erp.loan.dto;

import com.jdend.erp.loan.entity.LoanApplication;
import lombok.*;

import java.time.LocalDateTime;

public class LoanApplicationDtos {

    /** 업체가 ERP에서 제출하는 증차 자금 신청 */
    @Getter @Setter
    public static class SubmitRequest {
        /** FORM | PHONE */
        private String inquiryType;
        private String managerName;
        private String contactPhone;
        private String carModel;
        private Integer vehicleCount;
        private String expectedDelivery;
        private String financeType;
        private Integer downPaymentPercent;
        private Integer termMonths;
        private String memo;
        /** 금융사 제공 동의 */
        private boolean agreed;
    }

    @Getter @Builder
    public static class SubmitResponse {
        private Long id;
        private String message;
    }

    /** 신청 업체가 보는 내 신청 내역 */
    @Getter @Builder
    public static class MyRowResponse {
        private Long id;
        private String inquiryTypeLabel;
        private String carModel;
        private Integer vehicleCount;
        private String expectedDelivery;
        private String financeType;
        private Integer downPaymentPercent;
        private Integer termMonths;
        private String memo;
        private String status;
        private String statusLabel;
        /** 운영자가 남긴 안내 (운영자 내부 메모는 포함하지 않는다) */
        private String replyMessage;
        private LocalDateTime createdAt;

        public static MyRowResponse from(LoanApplication a) {
            return MyRowResponse.builder()
                    .id(a.getId())
                    .inquiryTypeLabel(label(a.getInquiryType()))
                    .carModel(a.getCarModel())
                    .vehicleCount(a.getVehicleCount())
                    .expectedDelivery(a.getExpectedDelivery())
                    .financeType(a.getFinanceType())
                    .downPaymentPercent(a.getDownPaymentPercent())
                    .termMonths(a.getTermMonths())
                    .memo(a.getMemo())
                    .status(a.getStatus())
                    .statusLabel(statusLabel(a.getStatus()))
                    .replyMessage(a.getReplyMessage())
                    .createdAt(a.getCreatedAt())
                    .build();
        }
    }

    /** 운영자가 보는 전체 신청 */
    @Getter @Builder
    public static class AdminRowResponse {
        private Long id;
        private Long companyId;
        private String companyName;
        private String requestedBy;
        private String inquiryTypeLabel;
        private String managerName;
        private String contactPhone;
        private String carModel;
        private Integer vehicleCount;
        private String expectedDelivery;
        private String financeType;
        private Integer downPaymentPercent;
        private Integer termMonths;
        private String memo;
        private String status;
        private String adminMemo;
        private String replyMessage;
        private LocalDateTime createdAt;

        public static AdminRowResponse from(LoanApplication a) {
            return AdminRowResponse.builder()
                    .id(a.getId())
                    .companyId(a.getCompanyId())
                    .companyName(a.getCompanyName())
                    .requestedBy(a.getRequestedBy())
                    .inquiryTypeLabel(label(a.getInquiryType()))
                    .managerName(a.getManagerName())
                    .contactPhone(a.getContactPhone())
                    .carModel(a.getCarModel())
                    .vehicleCount(a.getVehicleCount())
                    .expectedDelivery(a.getExpectedDelivery())
                    .financeType(a.getFinanceType())
                    .downPaymentPercent(a.getDownPaymentPercent())
                    .termMonths(a.getTermMonths())
                    .memo(a.getMemo())
                    .status(a.getStatus())
                    .adminMemo(a.getAdminMemo())
                    .replyMessage(a.getReplyMessage())
                    .createdAt(a.getCreatedAt())
                    .build();
        }
    }

    /** 운영자의 상태/메모 변경 */
    @Getter @Setter
    public static class UpdateRequest {
        private String status;
        private String adminMemo;
        private String replyMessage;
    }

    /** 신청 화면 상단에 보여줄 우리 회사 정보 (ERP에 이미 있는 값) */
    @Getter @Builder
    public static class MyCompanyResponse {
        private String companyName;
        private String managerName;
        private String contactPhone;
        /** 현재 보유 차량 대수 */
        private Long vehicleCount;
        /** 진행 중인 신청 건수 */
        private long openApplications;
    }

    /** 운영자 화면 요약 */
    @Getter @Builder
    public static class AdminSummaryResponse {
        private long newCount;
        private long reviewingCount;
        private long doneCount;
    }

    private static String label(String inquiryType) {
        return LoanApplication.TYPE_PHONE.equals(inquiryType) ? "유선상담" : "신청서";
    }

    private static String statusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case LoanApplication.STATUS_NEW       -> "접수";
            case LoanApplication.STATUS_REVIEWING -> "심사·알선중";
            case LoanApplication.STATUS_DONE      -> "실행완료";
            case LoanApplication.STATUS_REJECTED  -> "부결";
            case LoanApplication.STATUS_CANCELED  -> "취소";
            default -> status;
        };
    }
}
