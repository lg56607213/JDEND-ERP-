package com.jdend.erp.payment.banktx.service;

import com.jdend.erp.common.excel.ExcelReader;
import com.jdend.erp.common.excel.ExcelTemplateWriter;
import com.jdend.erp.payment.banktx.dto.*;
import com.jdend.erp.payment.banktx.entity.BankTransaction;
import com.jdend.erp.payment.banktx.repository.PaymentBankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class BankTransactionService {

  private final PaymentBankTransactionRepository repo;

  private static final List<String> EXCEL_HEADERS  = List.of("일자", "입금액", "출금액", "잔액", "적요");
  private static final List<String> EXCEL_SAMPLE   = List.of("2026-01-01", "1000000", "", "5000000", "월렌트료");

  /** 헤더명이 은행마다 달라 자주 쓰이는 표기를 표준 헤더로 매핑한다. */
  private static final Map<String, List<String>> HEADER_ALIASES = Map.of(
      "일자",   List.of("일자", "거래일자", "거래일", "날짜", "거래일시", "일시"),
      "입금액", List.of("입금액", "입금", "입금금액", "맡기신금액", "받으신금액", "입금(원)"),
      "출금액", List.of("출금액", "출금", "출금금액", "찾으신금액", "보내신금액", "출금(원)"),
      "잔액",   List.of("잔액", "거래후잔액", "잔액(원)", "거래후 잔액"),
      "적요",   List.of("적요", "내용", "거래내용", "메모", "받는통장표시", "의뢰인/수취인")
  );

  /** 헤더 행을 찾을 때 인정하는 모든 표기 */
  private static final List<String> HEADER_CANDIDATES = HEADER_ALIASES.values().stream()
      .flatMap(List::stream)
      .distinct()
      .toList();

  private static final int MAX_SKIP_REASONS = 20;

  @Transactional(readOnly = true)
  public List<BankTransactionRowResponse> search(String bank, String accountNo, LocalDate startDate, LocalDate endDate) {
    return repo.search(safe(bank), safe(accountNo), startDate, endDate).stream()
        .map(this::toRow)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<BankAccountPickRowResponse> distinctAccounts(String kw) {
    List<Object[]> rows = repo.distinctAccounts(safe(kw));
    List<BankAccountPickRowResponse> out = new ArrayList<>();
    for (Object[] r : rows) {
      out.add(BankAccountPickRowResponse.builder()
          .bankName((String) r[0])
          .accountNo((String) r[1])
          .build());
    }
    return out;
  }

  public byte[] template() {
    return ExcelTemplateWriter.write(EXCEL_HEADERS, EXCEL_SAMPLE);
  }

  /**
   * 은행 거래내역 엑셀 업로드.
   *
   * <p>중복 판정 규칙: (은행명, 계좌번호, 일자, 입금액, 출금액, 적요)가 같은 건이라도
   * <b>건수</b>까지 비교한다. 같은 날 같은 금액/적요의 거래가 실제로 3건이면 3건 모두 저장되고,
   * 같은 파일을 다시 올렸을 때만 중복으로 제외된다.</p>
   */
  public UploadResultResponse uploadExcel(String bankName, String accountNo, MultipartFile file) {
    if (file == null || file.isEmpty()) throw new RuntimeException("파일이 비어있습니다.");
    if (isBlank(bankName)) throw new RuntimeException("은행명을 선택하세요.");
    if (isBlank(accountNo)) throw new RuntimeException("계좌번호를 선택하세요.");

    String bn = bankName.trim();
    String an = accountNo.trim();
    String batchId = UUID.randomUUID().toString().replace("-", "");

    List<Map<String, String>> rawRows;
    try {
      rawRows = ExcelReader.readRows(file.getInputStream(), HEADER_CANDIDATES);
    } catch (Exception e) {
      throw new RuntimeException("엑셀 파일을 읽을 수 없습니다: " + e.getMessage());
    }

    if (rawRows.isEmpty()) {
      throw new RuntimeException("읽을 수 있는 데이터 행이 없습니다. 첫 번째 시트에 내역이 있는지 확인하세요.");
    }

    List<String> detectedHeaders = new ArrayList<>(rawRows.get(0).keySet());

    // 헤더 표준화 (은행별 표기 차이 흡수)
    List<Map<String, String>> rows = rawRows.stream().map(this::normalizeHeaders).toList();

    if (rows.stream().noneMatch(r -> r.containsKey("일자"))) {
      throw new RuntimeException("일자 열을 찾을 수 없습니다. 인식된 열: "
          + String.join(", ", detectedHeaders)
          + " / 필요한 열: " + String.join(", ", EXCEL_HEADERS));
    }

    // ── 1차 파싱 ────────────────────────────────────────────
    List<Parsed> parsedList = new ArrayList<>();
    List<String> skipReasons = new ArrayList<>();
    int skippedInvalid = 0;

    for (int i = 0; i < rows.size(); i++) {
      Map<String, String> row = rows.get(i);
      int dataRowNo = i + 1; // 헤더 아래 데이터 기준 순번
      String dateStr = safe(row.get("일자"));

      if (dateStr.isBlank()) {
        skippedInvalid++;
        addReason(skipReasons, dataRowNo + "번째 데이터 행: 일자가 비어 있어 제외");
        continue;
      }

      LocalDate txDate;
      try {
        txDate = parseDate(dateStr);
      } catch (Exception e) {
        skippedInvalid++;
        addReason(skipReasons, dataRowNo + "번째 데이터 행: 일자 형식을 인식할 수 없음 (" + dateStr + ")");
        continue;
      }

      long deposit    = parseMoney(row.get("입금액"));
      long withdrawal = parseMoney(row.get("출금액"));
      long balance    = parseMoney(row.get("잔액"));
      String summary  = safe(row.get("적요"));

      if (deposit == 0L && withdrawal == 0L) {
        skippedInvalid++;
        addReason(skipReasons, dataRowNo + "번째 데이터 행: 입금액과 출금액이 모두 0이어서 제외");
        continue;
      }

      parsedList.add(new Parsed(txDate, deposit, withdrawal, balance, summary, dataRowNo));
    }

    if (parsedList.isEmpty()) {
      return UploadResultResponse.builder()
          .batchId(batchId)
          .parsedRows(rows.size())
          .insertedRows(0)
          .skippedDuplicates(0)
          .skippedInvalid(skippedInvalid)
          .detectedHeaders(detectedHeaders)
          .skipReasons(skipReasons)
          .build();
    }

    // ── 기존 내역을 한 번에 읽어 건수 기준으로 중복 판정 ──────
    LocalDate minDate = parsedList.stream().map(Parsed::txDate).min(LocalDate::compareTo).orElseThrow();
    LocalDate maxDate = parsedList.stream().map(Parsed::txDate).max(LocalDate::compareTo).orElseThrow();

    Map<String, Integer> existingCount = new HashMap<>();
    for (BankTransaction t : repo.findForDuplicateCheck(bn, an, minDate, maxDate)) {
      String key = naturalKey(bn, an, t.getTxDate(), t.getDepositAmount(), t.getWithdrawalAmount(), t.getSummary());
      existingCount.merge(key, 1, Integer::sum);
    }

    Map<String, Integer> seenInFile = new HashMap<>();
    int inserted = 0, skippedDup = 0;

    for (Parsed p : parsedList) {
      String key = naturalKey(bn, an, p.txDate(), p.deposit(), p.withdrawal(), p.summary());
      int occurrence = seenInFile.merge(key, 1, Integer::sum) - 1; // 파일 내 0-based 등장 순번
      int already    = existingCount.getOrDefault(key, 0);

      if (occurrence < already) {
        // 동일한 거래가 이미 그 순번까지 등록되어 있음 → 실제 중복
        skippedDup++;
        continue;
      }

      String rowHash = sha256(key + "#" + occurrence);
      if (repo.existsByRowHash(rowHash)) { // unique 제약 위반 방지용 안전장치
        skippedDup++;
        continue;
      }

      repo.save(BankTransaction.builder()
          .bankName(bn)
          .accountNo(an)
          .txDate(p.txDate())
          .depositAmount(p.deposit())
          .withdrawalAmount(p.withdrawal())
          .balance(p.balance())
          .summary(p.summary())
          .importBatchId(batchId)
          .rowHash(rowHash)
          .build());
      inserted++;
    }

    return UploadResultResponse.builder()
        .batchId(batchId)
        .parsedRows(rows.size())
        .insertedRows(inserted)
        .skippedDuplicates(skippedDup)
        .skippedInvalid(skippedInvalid)
        .detectedHeaders(detectedHeaders)
        .skipReasons(skipReasons)
        .build();
  }

  public void updateRemarksBulk(List<RemarksUpdateRequest> list) {
    if (list == null || list.isEmpty()) return;
    for (RemarksUpdateRequest req : list) {
      if (req.getId() == null) continue;
      repo.findById(req.getId()).ifPresent(t ->
          t.setRemarks(req.getRemarks() == null ? "" : req.getRemarks().trim())
      );
    }
  }

  // ===== 내부 유틸 =====

  private record Parsed(LocalDate txDate, long deposit, long withdrawal, long balance,
                        String summary, int dataRowNo) {}

  private void addReason(List<String> reasons, String msg) {
    if (reasons.size() < MAX_SKIP_REASONS) reasons.add(msg);
  }

  /** 은행별 헤더 표기를 표준 헤더(일자/입금액/출금액/잔액/적요)로 변환 */
  private Map<String, String> normalizeHeaders(Map<String, String> row) {
    Map<String, String> out = new LinkedHashMap<>();
    row.forEach((k, v) -> {
      String key = normText(k);
      String mapped = null;
      for (Map.Entry<String, List<String>> e : HEADER_ALIASES.entrySet()) {
        if (e.getValue().stream().anyMatch(a -> normText(a).equals(key))) {
          mapped = e.getKey();
          break;
        }
      }
      out.putIfAbsent(mapped != null ? mapped : safe(k), safe(v));
    });
    return out;
  }

  /** 중복 판정 기준 키 (공백 차이는 무시) */
  private String naturalKey(String bank, String accountNo, LocalDate date,
                            Long deposit, Long withdrawal, String summary) {
    return String.join("|",
        normText(bank),
        normText(accountNo),
        date == null ? "" : date.toString(),
        String.valueOf(deposit == null ? 0L : deposit),
        String.valueOf(withdrawal == null ? 0L : withdrawal),
        normText(summary));
  }

  private String normText(String s) {
    return safe(s).replaceAll("\\s+", "");
  }

  private BankTransactionRowResponse toRow(BankTransaction t) {
    return BankTransactionRowResponse.builder()
        .id(t.getId())
        .bankName(t.getBankName())
        .accountNo(t.getAccountNo())
        .txDate(t.getTxDate())
        .depositAmount(t.getDepositAmount())
        .withdrawalAmount(t.getWithdrawalAmount())
        .balance(t.getBalance())
        .summary(t.getSummary())
        .remarks(t.getRemarks())
        .build();
  }

  private String safe(String s) { return s == null ? "" : s.trim(); }
  private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

  /** yyyy-MM-dd / yyyy.MM.dd / yyyy/MM/dd / yyyyMMdd, 뒤에 시각이 붙은 형태까지 허용 */
  private LocalDate parseDate(String s) {
    String v = safe(s);
    int sp = v.indexOf(' ');
    if (sp > 0) v = v.substring(0, sp); // "2026-01-05 13:22:11" → "2026-01-05"
    v = v.replace(".", "-").replace("/", "-").trim();
    while (v.endsWith("-")) v = v.substring(0, v.length() - 1);
    if (v.matches("^\\d{8}$")) {
      v = v.substring(0, 4) + "-" + v.substring(4, 6) + "-" + v.substring(6, 8);
    }
    String[] parts = v.split("-");
    if (parts.length == 3) { // 2026-1-5 처럼 0이 빠진 경우 보정
      v = String.format("%04d-%02d-%02d",
          Integer.parseInt(parts[0].trim()),
          Integer.parseInt(parts[1].trim()),
          Integer.parseInt(parts[2].trim()));
    }
    return LocalDate.parse(v, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  }

  private long parseMoney(String s) {
    String v = safe(s);
    if (v.isBlank() || v.equals("-")) return 0L;
    v = v.replace(",", "").replace("원", "").replace("₩", "").replace(" ", "");
    if (v.isBlank()) return 0L;
    if (v.startsWith("(") && v.endsWith(")")) v = "-" + v.substring(1, v.length() - 1);
    try { return Long.parseLong(v); } catch (Exception e) { return 0L; }
  }

  private String sha256(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] bytes = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : bytes) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException("해시 생성 실패: " + e.getMessage());
    }
  }
}
