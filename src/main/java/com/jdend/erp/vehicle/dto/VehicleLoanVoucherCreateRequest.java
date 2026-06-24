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
}