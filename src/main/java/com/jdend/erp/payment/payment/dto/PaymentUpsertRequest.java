package com.jdend.erp.payment.payment.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PaymentUpsertRequest {
  private String contractNumber;
  private LocalDate paymentDate;
  private Long paymentAmount;
  private String paymentMethod;
  private String companyAccount;
  private String memo;

  /**
   * true(기본/null) = 전표 생성 + 수납상태 변경
   * false           = 수납상태만 변경, 전표 미생성 (기존 데이터 컨버전용)
   */
  private Boolean createVoucher;
}