package com.jdend.erp.dashboard.service;

import com.jdend.erp.dashboard.dto.*;
import com.jdend.erp.dashboard.repository.*;
import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private final DashboardBankTransactionRepository bankTxRepo;
  private final ContractDashboardRepository contractRepo;
  private final VehicleInsuranceDashboardRepository insuranceRepo;
  private final MaturityDashboardRepository maturityRepo;
  private final ReceivableDashboardRepository receivableRepo;
  private final VehicleOrderRepository vehicleOrderRepo; // ✅ 추가

  public DashboardCashResponse cashDaily(LocalDate baseDate) {
    LocalDate d = (baseDate != null) ? baseDate : LocalDate.now().minusDays(1);

    long opening = nz(bankTxRepo.sumNetBefore(d));
    long dep = nz(bankTxRepo.sumDepositOn(d));
    long wit = nz(bankTxRepo.sumWithdrawalOn(d));
    long closing = opening + dep - wit;

    return DashboardCashResponse.builder()
        .baseDate(d)
        .openingBalance(opening)
        .todayDeposit(dep)
        .todayWithdrawal(wit)
        .closingBalance(closing)
        .build();
  }

  // ✅🔥 핵심 수정 부분
  public DashboardContractStatusResponse contractStatus() {

    // 전체 차량
    List<VehicleOrder> allVehicles = vehicleOrderRepo.findAll();

    // 장기 / 단기 차량번호
    Set<String> longSet = new HashSet<>(contractRepo.findLongTermVehicleNos());
    Set<String> shortSet = new HashSet<>(contractRepo.findShortTermVehicleNos());

    long longTerm = 0;
    long shortTerm = 0;
    long waiting = 0;

    for (VehicleOrder v : allVehicles) {

      String vehicleNo = normalize(v.getVehicleNo());

      if (vehicleNo == null) {
        waiting++;
        continue;
      }

      if (longSet.contains(vehicleNo)) {
        longTerm++;
      } else if (shortSet.contains(vehicleNo)) {
        shortTerm++;
      } else {
        waiting++;
      }
    }

    return DashboardContractStatusResponse.builder()
        .longTerm(longTerm)
        .shortTerm(shortTerm)
        .waiting(waiting)
        .build();
  }

  public List<DashboardInsuranceRow> insuranceExpiring(int days, int limit) {
    LocalDate today = LocalDate.now();
    LocalDate until = today.plusDays(days);

    List<DashboardInsuranceRow> rows = insuranceRepo.findInsuranceExpiring(today, until, limit);
    for (DashboardInsuranceRow r : rows) {
      r.setDday(r.getInsuranceEndDate() == null ? 0 : ChronoUnit.DAYS.between(today, r.getInsuranceEndDate()));
    }
    return rows;
  }

  public List<DashboardMaturityRow> maturitySoon(int days, int limit) {
    LocalDate today = LocalDate.now();
    LocalDate until = today.plusDays(days);

    List<DashboardMaturityRow> rows = maturityRepo.findMaturitySoon(today, until, limit);
    for (DashboardMaturityRow r : rows) {
      r.setDday(r.getEndDate() == null ? 0 : ChronoUnit.DAYS.between(today, r.getEndDate()));
    }
    return rows;
  }

  public List<DashboardReceivableRow> receivablesTop(int limit) {
    return receivableRepo.findTopReceivables(limit);
  }

  private long nz(Long v) {
    return v == null ? 0L : v;
  }

  // ✅ 차량번호 normalize
  private String normalize(String v) {
    if (v == null) return null;
    return v.replace(" ", "").replace("-", "").trim();
  }
}