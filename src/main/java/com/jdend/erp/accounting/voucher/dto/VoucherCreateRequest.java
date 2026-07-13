package com.jdend.erp.accounting.voucher.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherCreateRequest {
    private String voucherNo;
    private LocalDate voucherDate;

    private String contractNumber;
    private String vehicleNo;
    private String vehicleMgmtNo;
    private String memo;

    private List<VoucherLineRequest> debitEntries;
    private List<VoucherLineRequest> creditEntries;

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VoucherLineRequest {
        private String account;
        private Long amount;
        private String description;
    }
}