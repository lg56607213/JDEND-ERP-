package com.jdend.erp.payment.banktx.service;

import com.jdend.erp.payment.banktx.dto.*;
import com.jdend.erp.payment.banktx.entity.BankTransaction;
import com.jdend.erp.payment.banktx.repository.PaymentBankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class BankTransactionService {

  private final PaymentBankTransactionRepository repo;

  @Transactional(readOnly = true)
  public List<BankTransactionRowResponse> search(String bank, String accountNo, LocalDate startDate, LocalDate endDate) {
    String b = safe(bank);
    String a = safe(accountNo);
    return repo.search(b, a, startDate, endDate).stream()
        .map(this::toRow)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<BankAccountPickRowResponse> distinctAccounts(String kw) {
    String k = safe(kw);
    List<Object[]> rows = repo.distinctAccounts(k);

    List<BankAccountPickRowResponse> out = new ArrayList<>();
    for (Object[] r : rows) {
      out.add(BankAccountPickRowResponse.builder()
          .bankName((String) r[0])
          .accountNo((String) r[1])
          .build());
    }
    return out;
  }

  public UploadResultResponse uploadCsv(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new RuntimeException("CSV 파일이 비어있습니다.");
    }

    String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("");
    if (!filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
      throw new RuntimeException("CSV 파일만 업로드 가능합니다.");
    }

    String batchId = UUID.randomUUID().toString().replace("-", "");

    String text;
    try {
      byte[] bytes = file.getBytes();
      text = decodeCsv(bytes);
    } catch (Exception e) {
      throw new RuntimeException("파일 읽기 실패: " + e.getMessage());
    }

    List<List<String>> table = parseCsv(text);
    if (table.isEmpty()) {
      throw new RuntimeException("CSV 내용이 없습니다.");
    }

    List<String> header = table.get(0);
    Map<String, Integer> idx = headerIndex(header);

    int parsed = 0;
    int inserted = 0;
    int skipped = 0;

    for (int i = 1; i < table.size(); i++) {
      List<String> row = table.get(i);
      if (row == null || row.isEmpty()) continue;
      if (row.stream().allMatch(v -> safe(v).isBlank())) continue;

      parsed++;

      String bankName = get(row, idx, "bank");
      String accountNo = get(row, idx, "accountNo");
      String dateStr = get(row, idx, "date");
      String depositStr = get(row, idx, "deposit");
      String withdrawalStr = get(row, idx, "withdrawal");
      String summary = get(row, idx, "summary");
      String remarks = get(row, idx, "remarks");

      if (safe(bankName).isBlank() || safe(accountNo).isBlank() || safe(dateStr).isBlank()) {
        continue;
      }

      LocalDate txDate;
      try {
        txDate = parseDate(dateStr);
      } catch (Exception e) {
        continue;
      }

      long deposit = parseMoney(depositStr);
      long withdrawal = parseMoney(withdrawalStr);

      String rowHash = sha256(String.join("|",
          safe(bankName),
          safe(accountNo),
          String.valueOf(txDate),
          String.valueOf(deposit),
          String.valueOf(withdrawal),
          safe(summary)
      ));

      if (repo.existsByRowHash(rowHash)) {
        skipped++;
        continue;
      }

      repo.save(BankTransaction.builder()
          .bankName(safe(bankName))
          .accountNo(safe(accountNo))
          .txDate(txDate)
          .depositAmount(deposit)
          .withdrawalAmount(withdrawal)
          .summary(safe(summary))
          .remarks(safe(remarks))
          .importBatchId(batchId)
          .rowHash(rowHash)
          .build());

      inserted++;
    }

    return UploadResultResponse.builder()
        .batchId(batchId)
        .parsedRows(parsed)
        .insertedRows(inserted)
        .skippedDuplicates(skipped)
        .build();
  }

  // ✅ 핵심 수정
  // UTF-8이면 UTF-8로 읽고,
  // UTF-8로 깨지는 파일이면 CP949로 읽음.
  // 국내 은행 CSV는 대부분 CP949/MS949라서 이 처리가 필요함.
  private String decodeCsv(byte[] bytes) {
    if (bytes == null || bytes.length == 0) return "";

    if (hasUtf8Bom(bytes)) {
      return new String(bytes, StandardCharsets.UTF_8).replace("\uFEFF", "");
    }

    String utf8 = tryDecodeStrict(bytes, StandardCharsets.UTF_8);
    String cp949 = new String(bytes, Charset.forName("MS949"));

    if (utf8 == null) {
      return cp949.replace("\uFEFF", "");
    }

    int utf8Score = koreanCsvScore(utf8);
    int cp949Score = koreanCsvScore(cp949);

    if (cp949Score > utf8Score) {
      return cp949.replace("\uFEFF", "");
    }

    return utf8.replace("\uFEFF", "");
  }

  private String tryDecodeStrict(byte[] bytes, Charset charset) {
    try {
      CharsetDecoder decoder = charset.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT);

      CharBuffer decoded = decoder.decode(ByteBuffer.wrap(bytes));
      return decoded.toString();
    } catch (CharacterCodingException e) {
      return null;
    }
  }

  private int koreanCsvScore(String s) {
    if (s == null || s.isBlank()) return 0;

    int score = 0;

    String[] goodTokens = {
        "은행", "계좌", "일자", "거래일", "입금", "출금", "적요", "비고",
        "신한", "국민", "우리", "하나", "농협", "기업", "카카오", "토스"
    };

    for (String token : goodTokens) {
      if (s.contains(token)) score += 10;
    }

    int hangulCount = 0;
    int brokenCount = 0;

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);

      if (c >= '가' && c <= '힣') {
        hangulCount++;
      }

      if (c == '�') {
        brokenCount += 20;
      }
    }

    score += Math.min(hangulCount, 200);
    score -= brokenCount;

    return score;
  }

  private boolean hasUtf8Bom(byte[] bytes) {
    return bytes.length >= 3
        && (bytes[0] & 0xFF) == 0xEF
        && (bytes[1] & 0xFF) == 0xBB
        && (bytes[2] & 0xFF) == 0xBF;
  }

  private BankTransactionRowResponse toRow(BankTransaction t) {
    return BankTransactionRowResponse.builder()
        .id(t.getId())
        .bankName(t.getBankName())
        .accountNo(t.getAccountNo())
        .txDate(t.getTxDate())
        .depositAmount(t.getDepositAmount())
        .withdrawalAmount(t.getWithdrawalAmount())
        .summary(t.getSummary())
        .remarks(t.getRemarks())
        .build();
  }

  private String safe(String s) {
    return s == null ? "" : s.trim();
  }

  private List<List<String>> parseCsv(String text) {
    List<List<String>> out = new ArrayList<>();
    String[] lines = text.replace("\uFEFF", "").split("\\r?\\n");

    for (String line : lines) {
      if (line == null) continue;
      String ln = line.trim();
      if (ln.isEmpty()) continue;
      out.add(parseCsvLine(ln));
    }

    return out;
  }

  private List<String> parseCsvLine(String line) {
    List<String> cols = new ArrayList<>();
    StringBuilder sb = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);

      if (c == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          sb.append('"');
          i++;
        } else {
          inQuotes = !inQuotes;
        }
        continue;
      }

      if (c == ',' && !inQuotes) {
        cols.add(sb.toString().trim());
        sb.setLength(0);
        continue;
      }

      sb.append(c);
    }

    cols.add(sb.toString().trim());
    return cols;
  }

  private Map<String, Integer> headerIndex(List<String> header) {
    Map<String, Integer> map = new HashMap<>();

    for (int i = 0; i < header.size(); i++) {
      String h = normalize(header.get(i));

      if (h.contains("은행") || h.equals("bank") || h.contains("bankname")) map.put("bank", i);
      if (h.contains("계좌") || h.contains("account") || h.contains("acct")) map.put("accountNo", i);
      if (h.contains("일자") || h.contains("거래일") || h.equals("date") || h.contains("txdate")) map.put("date", i);
      if (h.contains("입금") || h.contains("deposit") || h.contains("in")) map.put("deposit", i);
      if (h.contains("출금") || h.contains("withdraw") || h.contains("out")) map.put("withdrawal", i);
      if (h.contains("적요") || h.contains("summary") || h.contains("memo")) map.put("summary", i);
      if (h.contains("비고") || h.contains("remark")) map.put("remarks", i);
    }

    map.putIfAbsent("bank", 0);
    map.putIfAbsent("accountNo", 1);
    map.putIfAbsent("date", 2);
    map.putIfAbsent("deposit", 3);
    map.putIfAbsent("withdrawal", 4);
    map.putIfAbsent("summary", 5);
    map.putIfAbsent("remarks", 6);

    return map;
  }

  private String normalize(String s) {
    return safe(s).toLowerCase(Locale.ROOT).replace(" ", "");
  }

  private String get(List<String> row, Map<String, Integer> idx, String key) {
    int i = idx.getOrDefault(key, -1);
    if (i < 0 || i >= row.size()) return "";
    return row.get(i);
  }

  private LocalDate parseDate(String s) {
    String v = safe(s)
        .replace(".", "-")
        .replace("/", "-")
        .replace(" ", "");

    if (v.matches("^\\d{8}$")) {
      v = v.substring(0, 4) + "-" + v.substring(4, 6) + "-" + v.substring(6, 8);
    }

    return LocalDate.parse(v, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  }

  private long parseMoney(String s) {
    String v = safe(s);
    if (v.isBlank() || v.equals("-")) return 0L;

    v = v.replace(",", "")
        .replace("원", "")
        .replace("₩", "")
        .replace(" ", "");

    if (v.isBlank()) return 0L;

    if (v.startsWith("(") && v.endsWith(")")) {
      v = "-" + v.substring(1, v.length() - 1);
    }

    try {
      return Long.parseLong(v);
    } catch (Exception e) {
      return 0L;
    }
  }

  private String sha256(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

      StringBuilder sb = new StringBuilder();
      for (byte b : bytes) {
        sb.append(String.format("%02x", b));
      }

      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException("해시 생성 실패: " + e.getMessage());
    }
  }
}