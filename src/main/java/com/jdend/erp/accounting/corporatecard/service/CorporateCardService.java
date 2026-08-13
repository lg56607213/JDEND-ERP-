package com.jdend.erp.accounting.corporatecard.service;

import com.jdend.erp.accounting.corporatecard.dto.CorporateCardRowResponse;
import com.jdend.erp.accounting.corporatecard.dto.CorporateCardSaveRequest;
import com.jdend.erp.accounting.corporatecard.dto.CorporateCardSummaryResponse;
import com.jdend.erp.accounting.corporatecard.entity.CorporateCardTransaction;
import com.jdend.erp.accounting.corporatecard.repository.CorporateCardTransactionRepository;
import com.jdend.erp.common.excel.ExcelReader;
import com.jdend.erp.common.excel.ExcelTemplateWriter;
import com.jdend.erp.common.excel.ExcelUploadResultResponse;
import com.jdend.erp.management.financial.entity.FinancialStatementAccount;
import com.jdend.erp.management.financial.repository.FinancialStatementAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.jdend.erp.common.excel.ExcelRowParsers.str;

/**
 * 법인카드 사용내역 관리.
 *
 * <p>흐름: 카드사 엑셀 업로드(사용일자/금액/적요/카드명) → 화면에서 내역 입력 + 계정분류 선택 → 저장.
 * 계정분류는 재무제표관리의 전기가능 계정코드를 그대로 쓴다.
 */
@Service
@RequiredArgsConstructor
public class CorporateCardService {

    /** 계정분류 필터에서 "미분류만" 을 뜻하는 값. */
    public static final String UNCLASSIFIED = "__NONE__";

    private static final List<String> HEADERS = List.of("사용일자", "금액", "적요", "카드명");

    private static final List<List<String>> SAMPLE_ROWS = List.of(
            List.of("2026-06-03", "55000",  "GS칼텍스 강남주유소", "신한 1234"),
            List.of("2026-06-11", "132000", "현대오토큐 정비",     "신한 1234"),
            // 취소·환불 건은 금액을 음수로 입력
            List.of("2026-06-15", "-32000", "쿠팡 결제취소",       "신한 1234")
    );

    /** 카드사마다 날짜 표기가 달라 흔한 형식은 모두 받아준다. */
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yy-MM-dd"),
            DateTimeFormatter.ofPattern("yy/MM/dd"),
            DateTimeFormatter.ofPattern("yy.MM.dd")
    );

    private final CorporateCardTransactionRepository repo;
    private final FinancialStatementAccountRepository accountRepo;

    // ==========================
    // 조회
    // ==========================

    @Transactional(readOnly = true)
    public List<CorporateCardRowResponse> search(LocalDate startDate, LocalDate endDate, String accountCode) {
        return repo.search(startDate, endDate, normalizeFilter(accountCode)).stream()
                .map(this::toRow)
                .toList();
    }

    /** 조회기간 내 계정별 집계. 계정분류가 안 된 건은 "미분류" 로 묶는다. */
    @Transactional(readOnly = true)
    public List<CorporateCardSummaryResponse> summary(LocalDate startDate, LocalDate endDate, String accountCode) {
        Map<String, long[]> agg = new LinkedHashMap<>();   // key → [건수, 금액합]
        Map<String, String> names = new HashMap<>();

        for (CorporateCardTransaction c : repo.search(startDate, endDate, normalizeFilter(accountCode))) {
            boolean classified = c.getAccountCode() != null && !c.getAccountCode().isBlank();
            String key = classified ? c.getAccountCode() : "";
            names.putIfAbsent(key, classified ? c.getAccountName() : "미분류");

            long[] v = agg.computeIfAbsent(key, k -> new long[2]);
            v[0] += 1;
            v[1] += c.getAmount() == null ? 0 : c.getAmount();
        }

        return agg.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(e -> CorporateCardSummaryResponse.builder()
                        .accountCode(e.getKey().isBlank() ? null : e.getKey())
                        .accountName(names.get(e.getKey()))
                        .count(e.getValue()[0])
                        .totalAmount(e.getValue()[1])
                        .build())
                .toList();
    }

    private String normalizeFilter(String accountCode) {
        return (accountCode == null || accountCode.isBlank()) ? "" : accountCode.trim();
    }

    private CorporateCardRowResponse toRow(CorporateCardTransaction c) {
        return CorporateCardRowResponse.builder()
                .id(c.getId())
                .useDate(c.getUseDate())
                .cardName(c.getCardName())
                .summary(c.getSummary())
                .amount(c.getAmount())
                .detail(c.getDetail())
                .accountCode(c.getAccountCode())
                .accountName(c.getAccountName())
                .build();
    }

    // ==========================
    // 엑셀 업로드
    // ==========================

    public byte[] template() {
        return ExcelTemplateWriter.writeMultiRow(HEADERS, SAMPLE_ROWS);
    }

    /** 파싱·검증만 하고 저장하지 않는다. */
    @Transactional(readOnly = true)
    public ExcelUploadResultResponse preview(MultipartFile file) {
        return process(file, false);
    }

    /** 검증을 통과한 행만 저장한다. */
    @Transactional
    public ExcelUploadResultResponse upload(MultipartFile file) {
        return process(file, true);
    }

    private ExcelUploadResultResponse process(MultipartFile file, boolean persist) {
        List<Map<String, String>> rows = readRows(file);

        int success = 0;
        List<ExcelUploadResultResponse.RowError> errors = new ArrayList<>();
        List<CorporateCardTransaction> toSave = new ArrayList<>();

        // 파일 안에서의 중복도 잡아야 하므로 이번 업로드분 키도 함께 모은다.
        Set<String> seenInFile = new HashSet<>();

        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            try {
                CorporateCardTransaction tx = toEntity(rows.get(i));

                String key = tx.getUseDate() + "|" + tx.getAmount() + "|" + (tx.getSummary() == null ? "" : tx.getSummary());
                if (!seenInFile.add(key)) {
                    throw new IllegalArgumentException("같은 파일 안에 사용일자·금액·적요가 동일한 행이 중복되어 있습니다.");
                }
                if (repo.countDuplicates(tx.getUseDate(), tx.getAmount(), tx.getSummary()) > 0) {
                    throw new IllegalArgumentException("이미 등록된 내역입니다(사용일자·금액·적요 동일).");
                }

                toSave.add(tx);
                success++;
            } catch (Exception e) {
                errors.add(ExcelUploadResultResponse.RowError.builder()
                        .rowNumber(rowNumber)
                        .message(rowNumber + "행: " + e.getMessage())
                        .build());
            }
        }

        if (persist && !toSave.isEmpty()) {
            repo.saveAll(toSave);
        }

        return ExcelUploadResultResponse.builder()
                .totalRows(rows.size())
                .successCount(success)
                .failCount(rows.size() - success)
                .errors(errors)
                .build();
    }

    private List<Map<String, String>> readRows(MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = ExcelReader.readRows(file.getInputStream());
        } catch (Exception e) {
            throw new IllegalArgumentException("엑셀 파일을 읽을 수 없습니다: " + e.getMessage());
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("업로드할 데이터가 없습니다. 첫 행은 헤더(사용일자/금액/적요/카드명)여야 합니다.");
        }
        return rows;
    }

    private CorporateCardTransaction toEntity(Map<String, String> row) {
        LocalDate useDate = parseDate(str(row, "사용일자"));
        if (useDate == null) {
            throw new IllegalArgumentException("사용일자는 필수입니다.");
        }

        Long amount = parseAmount(str(row, "금액"));
        if (amount == null || amount == 0) {
            throw new IllegalArgumentException("금액은 필수이며 0이 될 수 없습니다(취소 건은 음수로 입력).");
        }

        return CorporateCardTransaction.builder()
                .useDate(useDate)
                .amount(amount)
                .summary(cut(str(row, "적요"), 255))
                .cardName(cut(str(row, "카드명"), 60))
                .build();
    }

    private LocalDate parseDate(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(v, f);
            } catch (Exception ignored) { /* 다음 형식 시도 */ }
        }
        throw new IllegalArgumentException("'사용일자' 값이 올바른 날짜 형식(yyyy-MM-dd)이 아닙니다: " + v);
    }

    /** 카드사 엑셀은 "55,000" · "55000원" · "55000.0" 처럼 표기가 제각각이라 숫자만 남겨 파싱한다. */
    private Long parseAmount(String raw) {
        if (raw == null) return null;
        String v = raw.trim()
                .replace(",", "")
                .replace(" ", "")
                .replace("원", "")
                .replaceAll("\\.0+$", "");
        if (v.isEmpty()) return null;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'금액' 값이 올바른 숫자가 아닙니다: " + raw);
        }
    }

    private String cut(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }

    // ==========================
    // 내역/계정분류 저장
    // ==========================

    /** 화면에서 편집한 내역·계정분류를 일괄 저장하고 저장된 건수를 반환한다. */
    @Transactional
    public int save(CorporateCardSaveRequest req) {
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            return 0;
        }

        List<Long> ids = req.getItems().stream()
                .map(CorporateCardSaveRequest.Item::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, CorporateCardTransaction> byId = new HashMap<>();
        repo.findAllById(ids).forEach(c -> byId.put(c.getId(), c));

        // 같은 계정코드가 여러 행에 반복되므로 계정명 조회 결과를 재사용한다.
        Map<String, String> accountNameCache = new HashMap<>();

        int saved = 0;
        for (CorporateCardSaveRequest.Item item : req.getItems()) {
            CorporateCardTransaction tx = byId.get(item.getId());
            if (tx == null) continue;

            tx.setDetail(cut(trimToNull(item.getDetail()), 255));

            String code = trimToNull(item.getAccountCode());
            if (code == null) {
                tx.setAccountCode(null);
                tx.setAccountName(null);
            } else {
                tx.setAccountCode(code);
                tx.setAccountName(accountNameCache.computeIfAbsent(code, this::resolveAccountName));
            }
            saved++;
        }

        return saved;
    }

    private String resolveAccountName(String accountCode) {
        FinancialStatementAccount account = accountRepo.findByAccountCode(accountCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정코드입니다: " + accountCode));
        if (!"사용".equals(account.getIsPostable())) {
            throw new IllegalArgumentException("전표에 사용할 수 없는 계정코드입니다(전기가능=미사용): " + accountCode);
        }
        return account.getAccountName();
    }

    private String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    // ==========================
    // 삭제
    // ==========================

    @Transactional
    public int delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        List<CorporateCardTransaction> targets = repo.findAllById(ids);
        repo.deleteAll(targets);
        return targets.size();
    }
}
