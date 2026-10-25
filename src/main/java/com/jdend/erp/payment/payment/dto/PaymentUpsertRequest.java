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

  /**
   * true(기본/null) = 납기 전 금액을 넘는 초과분을 선수금으로 자동 등록
   * false           = 선수금 등록 안 함 (타 시스템 수납이력 이관용 — 납기 전에 낸 회차를 그대로 옮길 때)
   */
  private Boolean registerExcessAsPrepaid;
}