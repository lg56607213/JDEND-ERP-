package com.jdend.erp.accounting.cash.service;

import com.jdend.erp.accounting.cash.dto.*;
import com.jdend.erp.accounting.cash.repository.BankTransactionRepository;
import com.jdend.erp.accounting.cash.repository.VoucherCashAggRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DailyCashService {

  private final BankTransactionRepository bankRepo;
  private final VoucherCashAggRepository voucherCashRepo;

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

    // 은행(은행별)
    List<DailyFundReportResponse.BankRow> banks = new ArrayList<>();
    long bankIn=0, bankOut=0;

    for (var r : bankRepo.sumByBankOn(date)) {
      long inAmt = nz(r.getInAmt());
      long outAmt = nz(r.getOutAmt());
      bankIn += inAmt;
      bankOut += outAmt;

      banks.add(DailyFundReportResponse.BankRow.builder()
          .bankName(r.getBankName())
          .income(inAmt)
          .expense(outAmt)
          .build());
    }

    // 전표(현금성 계정만, 계정별 합계)
    List<VoucherCashAggRepository.AccountCashSumRow> vRows =
        voucherCashRepo.sumCashByAccountOn(date,
            CASH_KEYS[0], CASH_KEYS[1], CASH_KEYS[2], CASH_KEYS[3], CASH_KEYS[4], CASH_KEYS[5], CASH_KEYS[6]);

    List<DailyFundReportResponse.VoucherRow> vIn = new ArrayList<>();
    List<DailyFundReportResponse.VoucherRow> vOut = new ArrayList<>();
    long vInSum=0, vOutSum=0;

    for (var r : vRows) {
      long amt = nz(r.getAmt());
      String name = r.getAccountName() == null ? "" : r.getAccountName();

      DailyFundReportResponse.VoucherRow row = DailyFundReportResponse.VoucherRow.builder()
          .accountCode("") // 테이블에 없어서 공백
          .accountName(name)
          .amount(amt)
          .memo("") // 전표라인에 memo 컬럼 없음
          .build();

      if ("DEBIT".equalsIgnoreCase(r.getLineType())) {
        vIn.add(row);
        vInSum += amt;
      } else {
        vOut.add(row);
        vOutSum += amt;
      }
    }

    return DailyFundReportResponse.builder()
        .banks(banks)
        .bankIncomeTotal(bankIn)
        .bankExpenseTotal(bankOut)
        .voucherIncomes(vIn)
        .voucherIncomeTotal(vInSum)
        .voucherExpenses(vOut)
        .voucherExpenseTotal(vOutSum)
        .incomeDiff(bankIn - vInSum)
        .expenseDiff(bankOut - vOutSum)
        .build();
  }

  private long nz(Long v){ return v == null ? 0L : v; }
}