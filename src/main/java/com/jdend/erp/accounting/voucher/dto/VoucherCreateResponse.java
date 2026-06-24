package com.jdend.erp.accounting.voucher.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherCreateResponse {
    private Long id;
    private String voucherNo;
}