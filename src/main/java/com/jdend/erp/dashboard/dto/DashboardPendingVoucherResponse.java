package com.jdend.erp.dashboard.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Builder
public class DashboardPendingVoucherResponse {
    private int count;
    private List<Row> recent;

    @Getter @Builder
    public static class Row {
        private Long id;
        private String voucherNo;
        private LocalDate voucherDate;
        private Long totalAmount;
        private String memo;
    }
}
