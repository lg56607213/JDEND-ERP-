package com.jdend.erp.accounting.statements.service;

import com.jdend.erp.accounting.statements.dto.*;
import com.jdend.erp.accounting.statements.repository.StatementAggRepository;
import com.jdend.erp.management.financial.entity.FinancialStatementAccount;
import com.jdend.erp.management.financial.repository.FinancialStatementAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatementService {

  private final StatementAggRepository aggRepo;
  private final FinancialStatementAccountRepository accountRepo;

  private static final Set<String> CREDIT_NORMAL_CATEGORIES = Set.of("LIABILITY", "EQUITY", "REVENUE");

  private static final Map<String, String> CATEGORY_LABEL = Map.of(
      "ASSET", "자산",
      "LIABILITY", "부채",
      "EQUITY", "자본",
      "REVENUE", "수익",
      "EXPENSE", "비용"
  );

  // ==========================
  // 재무상태표
  // ==========================
  @Transactional(readOnly = true)
  public BalanceSheetResponse balance(LocalDate ref, String status) {
    if (ref == null) throw new IllegalArgumentException("referenceDate는 필수입니다.");

    List<StatementAggRepository.LineSumRow> rows = aggRepo.sumByAccountToDate(ref, status);
    Map<String, Long> debit = new HashMap<>();
    Map<String, Long> credit = new HashMap<>();
    splitByLineType(rows, debit, credit);

    List<FinancialStatementAccount> all = accountRepo.findAll();
    Map<Long, List<FinancialStatementAccount>> byParent = groupByParent(all);

    StatementNodeResponse asset = buildRootNode(all, byParent, "ASSET", debit, credit);
    StatementNodeResponse liability = buildRootNode(all, byParent, "LIABILITY", debit, credit);
    StatementNodeResponse equity = buildRootNode(all, byParent, "EQUITY", debit, credit);

    // 당기순이익 마감: 전표상 마감분개가 없어 수익/비용이 자본으로 넘어오지 않으면
    // 재무상태표가 '자산 = 부채 + 자본'을 만족하지 못한다(불균형 금액 = 순이익).
    // 마감분개(전표)를 만들지 않고, 계산 시점에 누적 순이익(수익-비용, 기준일까지)을 자본에 가산해 균형을 맞춘다.
    // (복식부기상 총차변=총대변이므로 자산 = 부채 + 자본 + (수익-비용) 이 성립한다.)
    StatementNodeResponse revenueToDate = buildRootNode(all, byParent, "REVENUE", debit, credit);
    StatementNodeResponse expenseToDate = buildRootNode(all, byParent, "EXPENSE", debit, credit);
    long netIncome = revenueToDate.getAmount() - expenseToDate.getAmount();

    java.util.List<StatementNodeResponse> equityChildren = new java.util.ArrayList<>(
        equity.getChildren() != null ? equity.getChildren() : java.util.List.of());
    equityChildren.add(StatementNodeResponse.builder()
        .accountCode(null)
        .accountName("당기순이익")
        .level(2)
        .amount(netIncome)
        .children(java.util.List.of())
        .build());

    StatementNodeResponse equityWithNetIncome = StatementNodeResponse.builder()
        .accountCode(equity.getAccountCode())
        .accountName(equity.getAccountName())
        .level(equity.getLevel())
        .amount(equity.getAmount() + netIncome)
        .children(equityChildren)
        .build();

    return BalanceSheetResponse.builder()
        .asset(asset)
        .liability(liability)
        .equity(equityWithNetIncome)
        .totalAsset(asset.getAmount())
        .totalLiability(liability.getAmount())
        .totalEquity(equityWithNetIncome.getAmount())
        .build();
  }

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
    splitByLineType(rows, debit, credit);

    List<FinancialStatementAccount> all = accountRepo.findAll();
    Map<Long, List<FinancialStatementAccount>> byParent = groupByParent(all);

    StatementNodeResponse revenue = buildRootNode(all, byParent, "REVENUE", debit, credit);
    StatementNodeResponse expense = buildRootNode(all, byParent, "EXPENSE", debit, credit);

    return IncomeStatementResponse.builder()
        .revenue(revenue)
        .expense(expense)
        .totalRevenue(revenue.getAmount())
        .totalExpense(expense.getAmount())
        .netIncome(revenue.getAmount() - expense.getAmount())
        .build();
  }

  // ==========================
  // 계정 상세내역 (코드 prefix로 하위 leaf 계정명 전체를 모아 조회)
  // ==========================
  @Transactional(readOnly = true)
  public List<BalanceDetailRowResponse> balanceDetails(String accountCode, LocalDate startDate, LocalDate referenceDate, String status) {
    if (referenceDate == null) {
      throw new IllegalArgumentException("referenceDate는 필수입니다.");
    }
    if (accountCode == null || accountCode.isBlank()) {
      throw new IllegalArgumentException("accountCode는 필수입니다.");
    }

    List<String> accountNames = accountRepo.findAll().stream()
        .filter(a -> a.getAccountCode().startsWith(accountCode))
        .map(FinancialStatementAccount::getAccountName)
        .toList();

    if (accountNames.isEmpty()) {
      return List.of();
    }

    return aggRepo.findBalanceDetails(startDate, referenceDate, accountNames, status);
  }

  // ==========================
  // helpers
  // ==========================
  private static void splitByLineType(
      List<StatementAggRepository.LineSumRow> rows,
      Map<String, Long> debit,
      Map<String, Long> credit
  ) {
    for (var r : rows) {
      String name = safe(r.getAccountName());
      long amt = r.getAmt() == null ? 0L : r.getAmt();
      if ("DEBIT".equalsIgnoreCase(r.getLineType())) debit.merge(name, amt, Long::sum);
      else credit.merge(name, amt, Long::sum);
    }
  }

  private static Map<Long, List<FinancialStatementAccount>> groupByParent(List<FinancialStatementAccount> all) {
    return all.stream()
        .filter(a -> a.getParentId() != null)
        .collect(Collectors.groupingBy(FinancialStatementAccount::getParentId));
  }

  private StatementNodeResponse buildRootNode(
      List<FinancialStatementAccount> all,
      Map<Long, List<FinancialStatementAccount>> byParent,
      String category,
      Map<String, Long> debit,
      Map<String, Long> credit
  ) {
    Optional<FinancialStatementAccount> root = all.stream()
        .filter(a -> a.getParentId() == null && category.equals(a.getCategory()))
        .findFirst();

    // 신규 회사(테넌트) DB는 재무제표관리에서 계정을 등록하기 전까지 대분류 자체가 없을 수 있다.
    // 이 경우 에러 대신 금액 0인 빈 대분류 노드를 내려준다(재무제표관리에서 계정을 등록하면 채워짐).
    if (root.isEmpty()) {
      return StatementNodeResponse.builder()
          .accountCode(null)
          .accountName(CATEGORY_LABEL.get(category))
          .level(1)
          .amount(0L)
          .children(List.of())
          .build();
    }

    return buildNode(root.get(), byParent, debit, credit, new java.util.HashSet<>());
  }

  private StatementNodeResponse buildNode(
      FinancialStatementAccount node,
      Map<Long, List<FinancialStatementAccount>> byParent,
      Map<String, Long> debit,
      Map<String, Long> credit,
      java.util.Set<Long> visiting
  ) {
    // parentId가 순환을 이루면 무한 재귀로 서버가 죽으므로, 방문 중인 노드를 추적해 끊는다.
    if (!visiting.add(node.getId())) {
      throw new IllegalStateException("계정 트리에 순환 참조가 있습니다. id=" + node.getId());
    }

    List<FinancialStatementAccount> childEntities = byParent.getOrDefault(node.getId(), List.of());
    List<StatementNodeResponse> children = childEntities.stream()
        .map(c -> buildNode(c, byParent, debit, credit, visiting))
        .toList();

    visiting.remove(node.getId());

    long ownAmount = signedNetAmount(node.getCategory(), node.getAccountName(), debit, credit);
    long childrenSum = children.stream().mapToLong(StatementNodeResponse::getAmount).sum();

    return StatementNodeResponse.builder()
        .accountCode(node.getAccountCode())
        .accountName(node.getAccountName())
        .level(node.getLevel())
        .amount(ownAmount + childrenSum)
        .children(children)
        .build();
  }

  // ASSET/EXPENSE는 차변 정상잔액(debit-credit), LIABILITY/EQUITY/REVENUE는 대변 정상잔액(credit-debit)
  private long signedNetAmount(String category, String accountName, Map<String, Long> debit, Map<String, Long> credit) {
    String name = safe(accountName);
    long d = debit.getOrDefault(name, 0L);
    long c = credit.getOrDefault(name, 0L);
    long net = d - c;
    return CREDIT_NORMAL_CATEGORIES.contains(category) ? -net : net;
  }

  // BUG-11: 계정명 중간 공백까지 정규화 — Map 키 생성/조회 양쪽에서 동일하게 적용되므로
  //          연속 공백 불일치로 인한 집계 누락을 방지한다.
  private static String safe(String s) {
    if (s == null) return "";
    return s.trim().replaceAll("\\s+", " ");
  }
}
