package com.jdend.erp.vehicle.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleLoanVoucherCreateRequest {
    public List<Long> loanIds;

    // 회계일자
    public LocalDate voucherDate;

    // 상환금액
    public Long amount;

    // 전표메모
    public String memo;

    /** 납부회차 (선택 입력, 컨버전 참고용) */
    public Integer installmentNo;

    /**
     * true(기본) = 전표 생성 + 납부상태 변경
     * false       = 납부상태만 변경, 전표 미생성 (기존 데이터 컨버전용)
     */
    public Boolean createVoucher;
}