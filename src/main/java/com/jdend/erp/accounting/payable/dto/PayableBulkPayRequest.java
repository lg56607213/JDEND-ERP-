package com.jdend.erp.accounting.payable.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class PayableBulkPayRequest {
    private List<Long> lineIds;
    private LocalDate payDate;
    private String bankAccount;
    private String memo;
}
