package com.jdend.erp.accounting.cash.service;

import com.jdend.erp.accounting.cash.dto.*;
import com.jdend.erp.accounting.cash.repository.BankTransactionRepository;
import com.jdend.erp.accounting.cash.repository.VoucherCashAggRepository;
import com.jdend.erp.myinfo.entity.BankAccount;
import com.jdend.erp.myinfo.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyCashService {

  private final BankTransactionRepository bankRepo;
  private final VoucherCashAggRepository voucherCashRepo;
  private final BankAccountRepository bankAccountRepo;

  // 현금성 계정 키워드(원하면 여기 수정)
  private static final String[] CASH_KEYS = {"현금","예금","보통예금","은행","국민","하나","신한"};

  @Transactional(readOnly = true)
  public DailyCashFlowMonthResponse month(String ym) {
    if (ym == null || ym.isBlank()) throw new IllegalArgumentException("month(YYYY-MM)은 필수입니다.");

    YearMonth yearMonth = YearMonth.parse(ym);
    LocalDate start = yearMonth.atDay(1);
    LocalDate end = yearMonth.atEndOfMonth();

    // 1) 은행 집계
    Map<LocalDate, long[]> bankMap = new HashMap<>(); // [0]=in, [1]=out
    for (var r : bankRepo.sumByDay(start, end)) {
      bankMap.put(r.getTxDate(), new long[]{nz(r.getInAmt()), nz(r.getOutAmt())});
    }

    // 2) 전표(현금성 계정만) 집계
    Map<LocalDate, long[]> voucherMap = new HashMap<>(); // [0]=in(DEBIT), [1]=out(CREDIT)
    List<VoucherCashAggRepository.DayCashSumRow> vRows =
        voucherCashRepo.sumCashByDay(start, end,
            CASH_KEYS[0], CASH_KEYS[1], CASH_KEYS[2], CASH_KEYS[3], CASH_KEYS[4], CASH_KEYS[5], CASH_KEYS[6]);

    for (var r : vRows) {
      LocalDate d = r.getVoucherDate();
      long amt = nz(r.getAmt());
      long[] arr = voucherMap.computeIfAbsent(d, k -> new long[]{0,0});
      if ("DEBIT".equalsIgnoreCase(r.getLineType())) arr[0] += amt;
      else arr[1] += amt;
    }

    // 3) 날짜별 full row 생성 (월의 모든 날짜)
    List<DailyCashFlowRowResponse> out = new ArrayList<>();
    long tBankIn=0, tBankOut=0, tVouIn=0, tVouOut=0, tDiffIn=0, tDiffOut=0;

    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
      long[] b = bankMap.getOrDefault(d, new long[]{0,0});
      long[] v = voucherMap.getOrDefault(d, new long[]{0,0});

      long diffIn = b[0] - v[0];
      long diffOut = b[1] - v[1];

      tBankIn += b[0];
      tBankOut += b[1];
      tVouIn += v[0];
      tVouOut += v[1];
      tDiffIn += diffIn;
      tDiffOut += diffOut;

      out.add(DailyCashFlowRowResponse.builder()
          .date(d)
          .bankIncome(b[0])
          .bankExpense(b[1])
          .voucherIncome(v[0])
          .voucherExpense(v[1])
          .diffIncome(diffIn)
          .diffExpense(diffOut)
          .build());
    }

    return DailyCashFlowMonthResponse.builder()
        .rows(out)
        .totalBankIncome(tBankIn)
        .totalBankExpense(tBankOut)
        .totalVoucherIncome(tVouIn)
        .totalVoucherExpense(tVouOut)
        .totalDiffIncome(tDiffIn)
        .totalDiffExpense(tDiffOut)
        .build();
  }

  @Transactional(readOnly = true)
  public DailyFundReportResponse daily(LocalDate date) {
    if (date == null) throw new IllegalArgumentException("date는 필수입니다.");

    // 전일잔액 (기준일 이전 누적, 통장별)
    Map<String, long[]> prevMap = new HashMap<>(); // bankName → [cumIn, cumOut]
    for (var r : bankRepo.sumCumulativeByBankBefore(date)) {
      prevMap.put(r.getBankName(), new long[]{nz(r.getInAmt()), nz(r.getOutAmt())});
    }

    // 당일 통장별 입출금
    Map<String, long[]> todayMap = new HashMap<>();
    for (var r : bankRepo.sumByBankOn(date)) {
      todayMap.put(r.getBankName(), new long[]{nz(r.getInAmt()), nz(r.getOutAmt())});
    }

    // 통장 이름 전체 집합 (전일에만 있거나 당일에만 있어도 모두 포함)
    Set<String> bankNames = new LinkedHashSet<>();
    bankNames.addAll(prevMap.keySet());
    bankNames.addAll(todayMap.keySet());

    List<DailyFundReportResponse.BankRow> banks = new ArrayList<>();
    long bankIn=0, bankOut=0, totalPrev=0, totalBalance=0;

    for (String bankName : bankNames) {
      long[] prev  = prevMap.getOrDefault(bankName, new long[]{0,0});
      long[] today = todayMap.getOrDefault(bankName, new long[]{0,0});

      long prevBalance = prev[0] - prev[1];
      long inAmt  = today[0];
      long outAmt = today[1];
      long balance = prevBalance + inAmt - outAmt;

      bankIn       += inAmt;
      bankOut      += outAmt;
      totalPrev    += prevBalance;
      totalBalance += balance;

      banks.add(DailyFundReportResponse.BankRow.builder()
          .bankName(bankName)
          .prevBalance(prevBalance)
          .income(inAmt)
          .expense(outAmt)
          .balance(balance)
          .build());
    }

    // 전표(현금성 계정만, 계정별 합계) — banks 비어있을 때 fallback보다 먼저 조회
    List<VoucherCashAggRepository.AccountCashSumRow> vRows =
        voucherCashRepo.sumCashByAccountOn(date,
            CASH_KEYS[0], CASH_KEYS[1], CASH_KEYS[2], CASH_KEYS[3], CASH_KEYS[4], CASH_KEYS[5], CASH_KEYS[6]);

    // 은행 CSV 없을 때: 전표 기준 보통예금(100101) 잔액으로 Section I 대체 (계좌별 분리 + 주계좌)
    if (banks.isEmpty()) {
      for (BankBalance b : voucherBankBalances(
              voucherCashRepo.findBankLinesBeforeDate(date),
              voucherCashRepo.findBankLinesOnDate(date))) {
        bankIn += b.in(); bankOut += b.out();
        totalPrev += b.opening(); totalBalance += b.balance();
        banks.add(DailyFundReportResponse.BankRow.builder()
            .bankName(b.label()).prevBalance(b.opening())
            .income(b.in()).expense(b.out()).balance(b.balance())
            .build());
      }
    }

    List<DailyFundReportResponse.VoucherRow> vIn = new ArrayList<>();
    List<DailyFundReportResponse.VoucherRow> vOut = new ArrayList<>();
    long vInSum=0, vOutSum=0;

    for (var r : vRows) {
      long amt = nz(r.getAmt());
      String name = r.getAccountName() == null ? "" : r.getAccountName();
      DailyFundReportResponse.VoucherRow row = DailyFundReportResponse.VoucherRow.builder()
          .accountCode("").accountName(name).amount(amt).memo("").build();
      if ("DEBIT".equalsIgnoreCase(r.getLineType())) { vIn.add(row); vInSum += amt; }
      else { vOut.add(row); vOutSum += amt; }
    }

    // 전표 전체 계정(II. 일일 자금 세부내역 — 모든 전표)
    List<DailyFundReportResponse.VoucherRow> allDebits  = new ArrayList<>();
    List<DailyFundReportResponse.VoucherRow> allCredits = new ArrayList<>();
    long allDebitSum=0, allCreditSum=0;

    for (var r : voucherCashRepo.sumAllByAccountOn(date)) {
      long amt = nz(r.getAmt());
      String name = r.getAccountName() == null ? "" : r.getAccountName();
      DailyFundReportResponse.VoucherRow row = DailyFundReportResponse.VoucherRow.builder()
          .accountCode("").accountName(name).amount(amt).memo("").build();
      if ("DEBIT".equalsIgnoreCase(r.getLineType())) { allDebits.add(row);  allDebitSum  += amt; }
      else                                            { allCredits.add(row); allCreditSum += amt; }
    }

    return DailyFundReportResponse.builder()
        .banks(banks)
        .bankPrevBalanceTotal(totalPrev)
        .bankIncomeTotal(bankIn)
        .bankExpenseTotal(bankOut)
        .bankBalanceTotal(totalBalance)
        .voucherIncomes(vIn)
        .voucherIncomeTotal(vInSum)
        .voucherExpenses(vOut)
        .voucherExpenseTotal(vOutSum)
        .incomeDiff(bankIn - vInSum)
        .expenseDiff(bankOut - vOutSum)
        .allVoucherDebits(allDebits)
        .allVoucherDebitTotal(allDebitSum)
        .allVoucherCredits(allCredits)
        .allVoucherCreditTotal(allCreditSum)
        .build();
  }

  // ==========================
  // III/IV. 월별 자금 현황
  // ==========================
  @Transactional(readOnly = true)
  public MonthlyFundReportResponse monthly(String ym) {
    if (ym == null || ym.isBlank()) throw new IllegalArgumentException("month(YYYY-MM)은 필수입니다.");

    YearMonth yearMonth = YearMonth.parse(ym);
    LocalDate monthStart = yearMonth.atDay(1);
    LocalDate monthEnd   = yearMonth.atEndOfMonth();

    // 전월잔액 (해당 월 1일 이전 누적, 통장별)
    Map<String, long[]> prevMap = new HashMap<>();
    for (var r : bankRepo.sumCumulativeByBankBefore(monthStart)) {
      prevMap.put(r.getBankName(), new long[]{nz(r.getInAmt()), nz(r.getOutAmt())});
    }

    // 해당 월 통장별 합계
    Map<String, long[]> monthMap = new HashMap<>();
    for (var r : bankRepo.sumByBankBetween(monthStart, monthEnd)) {
      monthMap.put(r.getBankName(), new long[]{nz(r.getInAmt()), nz(r.getOutAmt())});
    }

    Set<String> bankNames = new LinkedHashSet<>();
    bankNames.addAll(prevMap.keySet());
    bankNames.addAll(monthMap.keySet());

    List<MonthlyFundReportResponse.MonthBankRow> banks = new ArrayList<>();
    long totalPrev=0, totalIn=0, totalOut=0, totalBalance=0;

    for (String bankName : bankNames) {
      long[] prev  = prevMap.getOrDefault(bankName, new long[]{0,0});
      long[] month = monthMap.getOrDefault(bankName, new long[]{0,0});

      long prevBalance = prev[0] - prev[1];
      long income  = month[0];
      long expense = month[1];
      long balance = prevBalance + income - expense;

      totalPrev    += prevBalance;
      totalIn      += income;
      totalOut     += expense;
      totalBalance += balance;

      banks.add(MonthlyFundReportResponse.MonthBankRow.builder()
          .bankName(bankName)
          .prevBalance(prevBalance)
          .income(income)
          .expense(expense)
          .balance(balance)
          .build());
    }

    // 은행 CSV 없을 때: 일일자금일보와 동일하게 전표 기준 보통예금 잔액으로 대체 (계좌별 + 주계좌)
    if (banks.isEmpty()) {
      for (BankBalance b : voucherBankBalances(
              voucherCashRepo.findBankLinesBeforeDate(monthStart),
              voucherCashRepo.findBankLinesBetween(monthStart, monthEnd))) {
        totalPrev += b.opening(); totalIn += b.in();
        totalOut += b.out();      totalBalance += b.balance();
        banks.add(MonthlyFundReportResponse.MonthBankRow.builder()
            .bankName(b.label()).prevBalance(b.opening())
            .income(b.in()).expense(b.out()).balance(b.balance())
            .build());
      }
    }

    // IV. 월별 세부내역 (전표 현금성 계정, 계정별 집계) — 기존 호환 유지
    List<MonthlyFundReportResponse.VoucherRow> mvIn  = new ArrayList<>();
    List<MonthlyFundReportResponse.VoucherRow> mvOut = new ArrayList<>();
    long mvInSum=0, mvOutSum=0;

    for (var r : voucherCashRepo.sumCashByAccountBetween(monthStart, monthEnd,
        CASH_KEYS[0], CASH_KEYS[1], CASH_KEYS[2], CASH_KEYS[3], CASH_KEYS[4], CASH_KEYS[5], CASH_KEYS[6])) {
      long amt = nz(r.getAmt());
      String name = r.getAccountName() == null ? "" : r.getAccountName();
      if ("DEBIT".equalsIgnoreCase(r.getLineType())) {
        mvIn.add(MonthlyFundReportResponse.VoucherRow.builder().accountName(name).amount(amt).build());
        mvInSum += amt;
      } else {
        mvOut.add(MonthlyFundReportResponse.VoucherRow.builder().accountName(name).amount(amt).build());
        mvOutSum += amt;
      }
    }

    // 전체 계정(IV. 월별 자금 세부내역 — 모든 전표)
    List<MonthlyFundReportResponse.VoucherRow> allMvDebits  = new ArrayList<>();
    List<MonthlyFundReportResponse.VoucherRow> allMvCredits = new ArrayList<>();
    long allMvDebitSum=0, allMvCreditSum=0;

    for (var r : voucherCashRepo.sumAllByAccountBetween(monthStart, monthEnd)) {
      long amt = nz(r.getAmt());
      String name = r.getAccountName() == null ? "" : r.getAccountName();
      if ("DEBIT".equalsIgnoreCase(r.getLineType())) {
        allMvDebits.add(MonthlyFundReportResponse.VoucherRow.builder().accountName(name).amount(amt).build());
        allMvDebitSum += amt;
      } else {
        allMvCredits.add(MonthlyFundReportResponse.VoucherRow.builder().accountName(name).amount(amt).build());
        allMvCreditSum += amt;
      }
    }

    return MonthlyFundReportResponse.builder()
        .banks(banks)
        .totalPrevBalance(totalPrev)
        .totalIncome(totalIn)
        .totalExpense(totalOut)
        .totalBalance(totalBalance)
        .voucherIncomes(mvIn)
        .voucherIncomeTotal(mvInSum)
        .voucherExpenses(mvOut)
        .voucherExpenseTotal(mvOutSum)
        .allVoucherDebits(allMvDebits)
        .allVoucherDebitTotal(allMvDebitSum)
        .allVoucherCredits(allMvCredits)
        .allVoucherCreditTotal(allMvCreditSum)
        .build();
  }

  // ==========================
  // 보통예금 전표 → 계좌별 잔액 (계좌 미지정분은 '주계좌')
  // ==========================

  /** 계좌를 지정하지 않고 끊은 보통예금 전표(엑셀 일괄업로드 등)를 모아 보여주는 가상 공용계좌 이름. */
  public static final String MAIN_ACCOUNT_LABEL = "주계좌";

  private record BankBalance(String label, long opening, long in, long out) {
    long balance() { return opening + in - out; }
  }

  /**
   * 보통예금 전표 라인을 적요에 적힌 계좌번호로 등록계좌에 배분하고,
   * 어느 계좌에도 매칭되지 않는 라인(계좌 미지정·일괄업로드분)은 '주계좌'로 모은다.
   *
   * <p>예전에는 매칭 실패한 라인을 그냥 버려서 자금일보 보통예금 합계가
   * 재무상태표의 보통예금보다 작게 나왔다. 주계좌로 흡수해 합계를 일치시킨다.
   */
  private List<BankBalance> voucherBankBalances(List<Object[]> prevLines, List<Object[]> periodLines) {
    List<BankAccount> accounts = bankAccountRepo.findByIsActiveTrueOrderByIdAsc();

    Map<Long, long[]> byAccount = new LinkedHashMap<>(); // accountId → [전기이월, 입금, 출금]
    for (BankAccount a : accounts) byAccount.put(a.getId(), new long[3]);
    long[] main = new long[3]; // 계좌 미지정분

    for (Object[] row : prevLines) {
      BankAccount m = matchBankAccount((String) row[2], accounts);
      long[] t = (m != null) ? byAccount.get(m.getId()) : main;
      t[0] += "DEBIT".equals(row[0]) ? toLong(row[1]) : -toLong(row[1]);
    }
    for (Object[] row : periodLines) {
      BankAccount m = matchBankAccount((String) row[2], accounts);
      long[] t = (m != null) ? byAccount.get(m.getId()) : main;
      if ("DEBIT".equals(row[0])) t[1] += toLong(row[1]);
      else                        t[2] += toLong(row[1]);
    }

    List<BankBalance> out = new ArrayList<>();

    // 주계좌는 항상 맨 위. 등록계좌가 없으면 전체가 주계좌 한 행이 된다.
    if (accounts.isEmpty() || main[0] != 0 || main[1] != 0 || main[2] != 0) {
      out.add(new BankBalance(MAIN_ACCOUNT_LABEL, main[0], main[1], main[2]));
    }

    for (BankAccount a : accounts) {
      long[] t = byAccount.get(a.getId());
      String label = (a.getAccountAlias() != null && !a.getAccountAlias().isBlank())
          ? a.getAccountAlias() + " (" + a.getBankName() + ")"
          : a.getBankName();
      out.add(new BankBalance(label, t[0], t[1], t[2]));
    }

    return out;
  }

  private long nz(Long v){ return v == null ? 0L : v; }

  private long toLong(Object o) {
    if (o == null) return 0L;
    if (o instanceof Long l) return l;
    if (o instanceof Number n) return n.longValue();
    return 0L;
  }

  // BUG-10 수정: 짧은 계좌번호가 긴 계좌번호를 포함한 문자열에 오매칭되는 문제 방지
  // 계좌번호를 길이 내림차순으로 정렬 후 매칭하여 더 구체적인(긴) 번호가 먼저 체크되도록 함
  private BankAccount matchBankAccount(String description, List<BankAccount> accounts) {
    if (description == null || description.isBlank()) return null;
    return accounts.stream()
        .filter(a -> a.getAccountNumber() != null && !a.getAccountNumber().isBlank())
        .sorted(Comparator.comparingInt(a -> -a.getAccountNumber().length()))
        .filter(a -> description.contains(a.getAccountNumber()))
        .findFirst().orElse(null);
  }
}