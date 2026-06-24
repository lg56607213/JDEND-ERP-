// src/main/java/com/jdend/erp/vehicle/dto/VehicleLoanDetailResponse.java
package com.jdend.erp.vehicle.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleLoanDetailResponse {
  private Long id;

  // 표시용
  private String makerContractNo; // vehicle_orders.maker_contract_no
  private String vehicleMgmtNo;   // vehicle_loans.vehicle_mgmt_no
  private String vehicleNo;       // vehicle_orders.vehicle_no

  // loan
  private String loanType;
  private Long loanPrincipal;
  private Double loanInterest;
  private String financeName;

  private String repaymentMethod;
  private Integer repaymentPeriod;

  private Integer paymentDay;
  private String monthlyPayment;

  private String repaymentAccount;
  private Long remainingPrincipal;
  private LocalDate lastPaymentDate;

  private Boolean terminated;

  // 최근 전표(상환내역)
  private List<VehicleLoanVoucherRowResponse> recentVouchers;
}