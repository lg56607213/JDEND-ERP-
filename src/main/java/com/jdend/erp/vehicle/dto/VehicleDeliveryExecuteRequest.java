package com.jdend.erp.vehicle.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleDeliveryExecuteRequest {

  public String loanType;          // 금융리스/할부
  public Long loanPrincipal;       // 대출원금
  public Double loanInterest;      // 이자율(%)
  public String financeName;       // 금융사명
  public String repaymentMethod;   // 원리금균등상환/원금균등상환/만기일시상환
  public Integer repaymentPeriod;  // 개월
  public Long downPayment;         // 선납금
  public LocalDate startDate;      // 시작일
  public LocalDate endDate;        // 종료일
  public Long deposit;             // 보증금
  public Integer paymentDay;       // 매월 상환일자 (1~31)
  public String monthlyPayment;    // 예: ₩123,000

  // ✅ 추가: 상환계좌(loan_status에 표시)
  public String repaymentAccount;
}
