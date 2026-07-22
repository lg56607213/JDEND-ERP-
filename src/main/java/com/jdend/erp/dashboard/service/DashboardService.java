package com.jdend.erp.dashboard.service;

import com.jdend.erp.accounting.voucher.entity.Voucher;
import com.jdend.erp.accounting.voucher.repository.VoucherRepository;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.customer.CustomerRepository;
import com.jdend.erp.dashboard.dto.*;
import com.jdend.erp.dashboard.repository.*;
import com.jdend.erp.myinfo.entity.BankAccount;
import com.jdend.erp.myinfo.repository.BankAccountRepository;
import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.inspection.entity.VehicleInspection;
import com.jdend.erp.vehicle.inspection.repository.VehicleInspectionRepository;
import com.jdend.erp.vehicle.insurance.entity.VehicleInsurance;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private final DashboardBankTransactionRepository bankTxRepo;
  private final DashboardVoucherRepository voucherRepo;
  private final BankAccountRepository bankAccountRepo;
  private final ContractDashboardRepository contractRepo;
  private final VehicleInsuranceDashboardRepository insuranceRepo;
  private final MaturityDashboardRepository maturityRepo;
  private final ReceivableDashboardRepository receivableRepo;
  private final VehicleOrderRepository vehicleOrderRepo;
  private final VehicleInspectionRepository inspectionRepo;
  private final VoucherRepository voucherRepository;
  private final ContractRepository contractRepository;
  private final CustomerRepository customerRepository;

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

      // BUG-10차-05: 동일 차량이 장기+단기 계약을 동시에 보유하면 longTerm만 집계되던 문제 수정.
      // else-if 대신 독립 if로 각각 카운트하고, 어느 쪽에도 속하지 않을 때만 waiting으로 분류.
      boolean isLong  = longSet.contains(vehicleNo);
      boolean isShort = shortSet.contains(vehicleNo);
      if (isLong)  longTerm++;
      if (isShort) shortTerm++;
      if (!isLong && !isShort) waiting++;
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
      // BUG-5차-03: contractNumber로 고객명 조회
      if (r.getContractNumber() != null && !r.getContractNumber().isBlank()) {
        contractRepository.findByContractNumber(r.getContractNumber()).ifPresent(contract -> {
          if (contract.getCustomerNumber() != null && !contract.getCustomerNumber().isBlank()) {
            customerRepository.findByCustomerNumber(contract.getCustomerNumber())
                .ifPresent(customer -> r.setCustomerName(customer.getCustomerName()));
          }
        });
      }
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

  public List<DashboardBankSummaryRow> bankSummary() {
    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);
    LocalDate twoDaysAgo = today.minusDays(2);

    List<BankAccount> accounts = bankAccountRepo.findByIsActiveTrueOrderByIdAsc();
    List<DashboardBankSummaryRow> result = new ArrayList<>();

    for (BankAccount acct : accounts) {
      String no = acct.getAccountNumber();
      long bal2 = nz(bankTxRepo.sumNetUpToByAccount(no, twoDaysAgo));
      long dep  = nz(bankTxRepo.sumDepositOnByAccount(no, yesterday));
      long wit  = nz(bankTxRepo.sumWithdrawalOnByAccount(no, yesterday));
      long cur  = bal2 + dep - wit;

      result.add(DashboardBankSummaryRow.builder()
          .bankName(acct.getBankName())
          .accountNumber(no)
          .accountAlias(acct.getAccountAlias())
          .balance2DaysAgo(bal2)
          .yesterdayDeposit(dep)
          .yesterdayWithdrawal(wit)
          .currentBalance(cur)
          .build());
    }
    return result;
  }

  public List<DashboardBankDiffRow> bankVoucherDiff() {
    LocalDate to = LocalDate.now().minusDays(1);
    LocalDate from = to.minusDays(13); // 최근 14일

    // 은행내역 일별 합계
    List<Object[]> bankRows = bankTxRepo.sumByDateRange(from, to);
    Map<LocalDate, long[]> bankMap = new LinkedHashMap<>();
    for (Object[] row : bankRows) {
      LocalDate d = ((java.sql.Date) row[0]).toLocalDate();
      long dep = toLong(row[1]);
      long wit = toLong(row[2]);
      bankMap.put(d, new long[]{dep, wit});
    }

    // 전표내역 일별 합계
    List<Object[]> voucherRows = voucherRepo.sumBankVoucherByDateRange(from, to);
    Map<LocalDate, long[]> voucherMap = new LinkedHashMap<>();
    for (Object[] row : voucherRows) {
      LocalDate d = ((java.sql.Date) row[0]).toLocalDate();
      long debit  = toLong(row[1]);
      long credit = toLong(row[2]);
      voucherMap.put(d, new long[]{debit, credit});
    }

    // 차이가 있는 날짜만 수집
    Set<LocalDate> allDates = new LinkedHashSet<>();
    allDates.addAll(bankMap.keySet());
    allDates.addAll(voucherMap.keySet());

    List<DashboardBankDiffRow> result = new ArrayList<>();
    for (LocalDate d : allDates) {
      long[] b = bankMap.getOrDefault(d, new long[]{0, 0});
      long[] v = voucherMap.getOrDefault(d, new long[]{0, 0});
      long depDiff = b[0] - v[0];
      long witDiff = b[1] - v[1];
      if (depDiff != 0 || witDiff != 0) {
        result.add(DashboardBankDiffRow.builder()
            .txDate(d)
            .bankDeposit(b[0])
            .bankWithdrawal(b[1])
            .voucherDeposit(v[0])
            .voucherWithdrawal(v[1])
            .depositDiff(depDiff)
            .withdrawalDiff(witDiff)
            .build());
      }
    }
    result.sort(Comparator.comparing(DashboardBankDiffRow::getTxDate).reversed());
    return result;
  }

  private long toLong(Object o) {
    if (o == null) return 0L;
    if (o instanceof Long) return (Long) o;
    if (o instanceof BigDecimal) return ((BigDecimal) o).longValue();
    if (o instanceof Number) return ((Number) o).longValue();
    return 0L;
  }

  private long nz(Long v) {
    return v == null ? 0L : v;
  }

  /** 등록된 전체 차량의 보험 가입 현황 (미가입 포함) */
  public List<DashboardVehicleInsuranceRow> vehicleInsuranceAll() {
    List<VehicleOrder> vehicles = vehicleOrderRepo.findAll().stream()
        .filter(o -> o.getVehicleNo() != null && !o.getVehicleNo().isBlank())
        .toList();

    // 차량번호 기준 최신 보험 종료일
    Map<String, LocalDate> latestEnd = new HashMap<>();
    for (VehicleInsurance i : insuranceRepo.findAll()) {
      if (i.getVehicleNo() == null) continue;
      String key = normalize(i.getVehicleNo());
      LocalDate cur = latestEnd.get(key);
      if (cur == null || i.getInsuranceEndDate().isAfter(cur)) {
        latestEnd.put(key, i.getInsuranceEndDate());
      }
    }

    LocalDate today = LocalDate.now();
    List<DashboardVehicleInsuranceRow> result = new ArrayList<>();

    for (VehicleOrder o : vehicles) {
      String key = normalize(o.getVehicleNo());
      LocalDate endDate = latestEnd.get(key);

      String status;
      Long dday = null;
      if (endDate == null) {
        status = "미가입";
      } else {
        dday = ChronoUnit.DAYS.between(today, endDate);
        if (dday < 0)       status = "만료";
        else if (dday <= 30) status = "만료임박";
        else                 status = "정상";
      }

      result.add(DashboardVehicleInsuranceRow.builder()
          .vehicleMgmtNo(o.getVehicleMgmtNo())
          .vehicleNo(o.getVehicleNo())
          .carModel(o.getCarModel())
          .insuranceEndDate(endDate)
          .status(status)
          .dday(dday)
          .build());
    }

    // 만료 → 만료임박 → 미가입 → 정상 순 정렬
    result.sort(Comparator.comparingInt((DashboardVehicleInsuranceRow r) -> statusOrder(r.getStatus()))
        .thenComparing(r -> r.getInsuranceEndDate() == null ? LocalDate.MAX : r.getInsuranceEndDate()));
    return result;
  }

  /** 등록된 전체 차량의 정기검사 현황 (미등록 포함) */
  public List<DashboardVehicleInspectionRow> vehicleInspectionAll() {
    List<VehicleOrder> vehicles = vehicleOrderRepo.findAll().stream()
        .filter(o -> o.getVehicleNo() != null && !o.getVehicleNo().isBlank())
        .toList();

    // 차량관리번호 기준 최신 검사 종료일 (vehicle_inspections 테이블 우선)
    Map<String, LocalDate> latestInspEnd = new HashMap<>();
    for (VehicleInspection vi : inspectionRepo.findAll()) {
      if (vi.getVehicleMgmtNo() == null || vi.getValidEnd() == null) continue;
      String key = normalize(vi.getVehicleMgmtNo());
      LocalDate cur = latestInspEnd.get(key);
      if (cur == null || vi.getValidEnd().isAfter(cur)) {
        latestInspEnd.put(key, vi.getValidEnd());
      }
    }

    LocalDate today = LocalDate.now();
    List<DashboardVehicleInspectionRow> result = new ArrayList<>();

    for (VehicleOrder o : vehicles) {
      String key = normalize(o.getVehicleMgmtNo());
      // vehicle_inspections 우선, 없으면 등록 시 기재한 inspection_end
      LocalDate endDate = latestInspEnd.getOrDefault(key, o.getInspectionEnd());

      String status;
      Long dday = null;
      if (endDate == null) {
        status = "미등록";
      } else {
        dday = ChronoUnit.DAYS.between(today, endDate);
        if (dday < 0)       status = "만료";
        else if (dday <= 30) status = "만료임박";
        else                 status = "정상";
      }

      result.add(DashboardVehicleInspectionRow.builder()
          .vehicleMgmtNo(o.getVehicleMgmtNo())
          .vehicleNo(o.getVehicleNo())
          .carModel(o.getCarModel())
          .inspectionEndDate(endDate)
          .status(status)
          .dday(dday)
          .build());
    }

    result.sort(Comparator.comparingInt((DashboardVehicleInspectionRow r) -> statusOrder(r.getStatus()))
        .thenComparing(r -> r.getInspectionEndDate() == null ? LocalDate.MAX : r.getInspectionEndDate()));
    return result;
  }

  private int statusOrder(String status) {
    return switch (status) {
      case "만료"   -> 0;
      case "만료임박" -> 1;
      case "미가입", "미등록" -> 2;
      default       -> 3; // 정상
    };
  }

  public DashboardPendingVoucherResponse pendingVoucherSummary() {
    List<Voucher> pending = voucherRepository.searchForApproval(null, "대기");
    int count = pending.size();
    List<DashboardPendingVoucherResponse.Row> recent = pending.stream()
        .limit(5)
        .map(v -> DashboardPendingVoucherResponse.Row.builder()
            .id(v.getId())
            .voucherNo(v.getVoucherNo())
            .voucherDate(v.getVoucherDate())
            .totalAmount(v.getTotalAmount())
            .memo(v.getMemo())
            .build())
        .toList();
    return DashboardPendingVoucherResponse.builder()
        .count(count)
        .recent(recent)
        .build();
  }

  // ✅ 차량번호 normalize
  private String normalize(String v) {
    if (v == null) return null;
    return v.replace(" ", "").replace("-", "").trim();
  }
}