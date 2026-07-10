package com.jdend.erp.payment.banktx.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RemarksUpdateRequest {
    private Long id;
    private String remarks;
}
