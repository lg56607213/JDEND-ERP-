package com.jdend.erp.accounting.voucher.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdListRequest {
    private List<Long> ids;
}