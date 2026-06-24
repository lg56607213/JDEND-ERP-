package com.jdend.erp.accounting.statements.service;

import com.jdend.erp.accounting.statements.dto.*;
import com.jdend.erp.accounting.statements.mapper.AccountGroupMapper;
import com.jdend.erp.accounting.statements.repository.StatementAggRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatementService {

  private final StatementAggRepository aggRepo;

  // ==========================
  // 손익계산서
  // ==========================
  @Transactional(readOnly = true)
  public IncomeStatementResponse income(LocalDate start, LocalDate end, String status) {
    if (start == null || end == null) throw new IllegalArgumentException("startDate/endDate는 필수입니다.");
    if (start.isAfter(end)) throw new IllegalArgumentException("startDate는 endDate보다 이후일 수 없습니다.");

    List<StatementAggRepository.LineSumRow> rows = aggRepo.sumByAccountBetween(start, end, status);

    Map<String, Long> debit = new HashMap<>();
    Map<String, Long> credit = new HashMap<>();

    for (var r : rows) {
      String name = safe(r.getAccountName());
      long amt = r.getAmt() == null ? 0L : r.getAmt();
      if ("DEBIT".equalsIgnoreCase(r.getLineType())) debit.merge(name, amt, Long::sum);
      else credit.merge(name, amt, Long::sum);
    }

    long revenue = sumByKeywords(credit, "매출", "매출액", "렌트매출", "리스매출", "렌트료수익");
    long costOfSales = sumByKeywords(debit, "매출원가", "원가");

    long salary = sumByKeywords(debit, "급여");
    long bonus = sumByKeywords(debit, "상여");
    long welfare = sumByKeywords(debit, "복리후생");
    long travel = sumByKeywords(debit, "여비", "교통");
    long entertainment = sumByKeywords(debit, "접대");
    long communication = sumByKeywords(debit, "통신");
    long utilities = sumByKeywords(debit, "수도", "광열");
    long taxes = sumByKeywords(debit, "세금", "공과");
    long depreciation = sumByKeywords(debit, "감가상각");
    long insurance = sumByKeywords(debit, "보험");
    long rent = sumByKeywords(debit, "임차", "임대료");
    long vehicleMaintenance = sumByKeywords(debit, "차량유지", "정비");
    long fees = sumByKeywords(debit, "수수료", "지급수수료");

    long nonOpRev = sumByKeywords(credit, "영업외수익", "이자수익", "잡이익");
    long nonOpExp = sumByKeywords(debit, "영업외비용", "이자비용", "잡손실");
    long corporateTax = sumByKeywords(debit, "법인세");

    return IncomeStatementResponse.builder()
        .revenue(revenue)
        .costOfSales(costOfSales)
        .salary(salary)
        .bonus(bonus)
        .welfare(welfare)
        .travel(travel)
        .entertainment(entertainment)
        .communication(communication)
        .utilities(utilities)
        .taxes(taxes)
        .depreciation(depreciation)
        .insurance(insurance)
        .rent(rent)
        .vehicleMaintenance(vehicleMaintenance)
        .fees(fees)
        .nonOperatingRevenue(nonOpRev)
        .nonOperatingExpense(nonOpExp)
        .corporateTax(corporateTax)
        .build();
  }

  // ==========================
  // 재무상태표
  // ==========================
  @Transactional(readOnly = true)
  public BalanceSheetResponse balance(LocalDate ref, String status) {
    if (ref == null) throw new IllegalArgumentException("referenceDate는 필수입니다.");

    List<StatementAggRepository.LineSumRow> rows = aggRepo.sumByAccountToDate(ref, status);

    Map<String, Long> debit = new HashMap<>();
    Map<String, Long> credit = new HashMap<>();

    for (var r : rows) {
      String name = safe(r.getAccountName());
      long amt = r.getAmt() == null ? 0L : r.getAmt();
      if ("DEBIT".equalsIgnoreCase(r.getLineType())) debit.merge(name, amt, Long::sum);
      else credit.merge(name, amt, Long::sum);
    }

    long cash = calcGroupBalance("cash", debit, credit);
    long vatReceivable = calcGroupBalance("vatReceivable", debit, credit);
    long prepaidAssets = calcGroupBalance("prepaidAssets", debit, credit);

    long rentAssetsStatus = calcGroupBalance("rentAssetsStatus", debit, credit);
    long accumulatedDepreciation = calcGroupBalance("accumulatedDepreciation", debit, credit);
    if (accumulatedDepreciation > 0) {
      accumulatedDepreciation = -accumulatedDepreciation;
    }

    long rentAssets = rentAssetsStatus + accumulatedDepreciation + prepaidAssets;

    long borrowed = calcGroupBalance("borrowedLiabilities", debit, credit);
    long advance = calcGroupBalance("advanceReceived", debit, credit);
    long accountsPayable = calcGroupBalance("accountsPayable", debit, credit);
    long deposit = calcGroupBalance("deposit", debit, credit);

    long capital = calcGroupBalance("capital", debit, credit);
    long capitalSurplus = calcGroupBalance("capitalSurplus", debit, credit);

    return BalanceSheetResponse.builder()
        .cash(cash)
        .rentAssetsStatus(rentAssetsStatus)
        .accumulatedDepreciation(accumulatedDepreciation)
        .prepaidAssets(prepaidAssets)
        .vatReceivable(vatReceivable)
        .rentAssets(rentAssets)
        .borrowedLiabilities(borrowed)
        .advanceReceived(advance)
        .accountsPayable(accountsPayable)
        .deposit(deposit)
        .capital(capital)
        .capitalSurplus(capitalSurplus)
        .build();
  }

  // ==========================
  // 재무상태표 상세내역
  // ==========================
  @Transactional(readOnly = true)
  public List<BalanceDetailRowResponse> balanceDetails(String groupKey, LocalDate referenceDate, String status) {
    if (referenceDate == null) {
      throw new IllegalArgumentException("referenceDate는 필수입니다.");
    }

    List<String> accountNames = AccountGroupMapper.getBalanceAccounts(groupKey);
    return aggRepo.findBalanceDetails(referenceDate, accountNames, status);
  }

  // ==========================
  // helpers
  // ==========================
  private static String safe(String s) {
    return s == null ? "" : s.trim();
  }

  private static long sumByKeywords(Map<String, Long> map, String... keywords) {
    long sum = 0;
    for (var e : map.entrySet()) {
      String name = e.getKey();
      for (String k : keywords) {
        if (!k.isBlank() && name.contains(k)) {
          sum += (e.getValue() == null ? 0L : e.getValue());
          break;
        }
      }
    }
    return sum;
  }

  private long calcGroupBalance(String groupKey, Map<String, Long> debit, Map<String, Long> credit) {
    List<String> names = AccountGroupMapper.getBalanceAccounts(groupKey);
    boolean isAsset = AccountGroupMapper.isAssetGroup(groupKey);

    long debitSum = sumByExactOrContains(debit, names);
    long creditSum = sumByExactOrContains(credit, names);

    return isAsset ? (debitSum - creditSum) : (creditSum - debitSum);
  }

  private long sumByExactOrContains(Map<String, Long> map, List<String> names) {
    long sum = 0;

    for (var e : map.entrySet()) {
      String actual = safe(e.getKey());
      long amt = e.getValue() == null ? 0L : e.getValue();

      for (String target : names) {
        String t = safe(target);
        if (actual.equals(t) || actual.contains(t) || t.contains(actual)) {
          sum += amt;
          break;
        }
      }
    }

    return sum;
  }
}