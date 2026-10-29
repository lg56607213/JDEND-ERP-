package com.jdend.erp.vehicle.fine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdend.erp.common.pdf.PdfMerger;
import com.jdend.erp.common.pdf.PdfRenderer;
import com.jdend.erp.document.entity.VehicleDocument;
import com.jdend.erp.document.service.VehicleDocumentService;
import com.jdend.erp.vehicle.fine.dto.FineAutomationDtos.*;
import com.jdend.erp.vehicle.fine.dto.TrafficFineDtos.ContractMatch;
import com.jdend.erp.vehicle.fine.dto.TrafficFineDtos.Response;
import com.jdend.erp.vehicle.fine.dto.TrafficFineDtos.SaveRequest;
import com.jdend.erp.vehicle.fine.entity.FaxSetting;
import com.jdend.erp.vehicle.fine.entity.TrafficFine;
import com.jdend.erp.vehicle.fine.entity.TrafficFineFax;
import com.jdend.erp.vehicle.fine.entity.TrafficFineUpload;
import com.jdend.erp.vehicle.fine.repository.FaxSettingRepository;
import com.jdend.erp.vehicle.fine.repository.TrafficFineFaxRepository;
import com.jdend.erp.vehicle.fine.repository.TrafficFineRepository;
import com.jdend.erp.vehicle.fine.repository.TrafficFineUploadRepository;
import com.popbill.api.PopbillException;
import com.popbill.api.fax.FaxResult;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 과태료 고지서 일괄 업로드(사진 자동 인식) · 변경부과 신청 팩스 발송.
 * 업로드와 팩스는 성공·실패 모두 기록(traffic_fine_uploads, traffic_fine_faxes)으로 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficFineAutomationService {

  private static final Pattern PLATE = Pattern.compile("^(\\d{2,3}[가-힣]\\d{4})");
  private static final DateTimeFormatter AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final DateTimeFormatter POPBILL_DT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final Set<String> PENDING = Set.of("접수", "전송중");

  private final TrafficFineService fineService;
  private final TrafficFineRepository fineRepo;
  private final TrafficFineUploadRepository uploadRepo;
  private final TrafficFineFaxRepository faxRepo;
  private final FaxSettingRepository settingRepo;
  private final VehicleDocumentService documentService;
  private final FineNoticeReader reader;
  private final FaxSender faxSender;
  private final PdfRenderer pdfRenderer;
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  /** 고지서 인식은 외부 API 호출만 하므로 병렬로 돌린다(DB 작업은 요청 스레드에서). */
  private final ExecutorService readers = Executors.newFixedThreadPool(4);

  @PreDestroy
  void shutdown() {
    readers.shutdownNow();
  }

  // ───────────────────────── 일괄 업로드 ─────────────────────────

  public BulkUploadResponse bulkUpload(List<MultipartFile> files, String loginId) {
    if (files == null || files.isEmpty()) throw new IllegalArgumentException("업로드할 파일이 없습니다.");
    String batchId = UUID.randomUUID().toString();

    List<Future<FineNoticeReader.Notice>> reads = new ArrayList<>();
    for (MultipartFile f : files) {
      byte[] bytes;
      try {
        bytes = f.getBytes();
      } catch (IOException e) {
        bytes = new byte[0];
      }
      final byte[] b = bytes;
      final String ext = VehicleDocumentService.extractExtension(f.getOriginalFilename());
      reads.add(reader.configured() && b.length > 0 ? readers.submit(() -> reader.read(b, ext)) : null);
    }

    List<UploadResult> results = new ArrayList<>();
    for (int i = 0; i < files.size(); i++) {
      results.add(processOne(batchId, files.get(i), reads.get(i), loginId));
    }
    return new BulkUploadResponse(batchId, results);
  }

  private UploadResult processOne(String batchId, MultipartFile file,
                                  Future<FineNoticeReader.Notice> read, String loginId) {
    String fileName = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
    String filePlate = plateOf(fileName);
    FineNoticeReader.Notice n = null;
    List<String> issues = new ArrayList<>();
    if (read == null) {
      issues.add(reader.configured() ? "빈 파일" : "자동 인식 키 미설정 — 고지서 내용을 직접 입력해 주세요");
    } else {
      try {
        n = read.get(180, TimeUnit.SECONDS);
      } catch (Exception e) {
        issues.add("고지서 인식 실패: " + rootMessage(e));
      }
    }

    String notePlate = n == null ? null : normPlate(n.vehicleNo());
    String vehicleNo = filePlate != null ? filePlate : notePlate;
    if (vehicleNo == null) {
      return log(batchId, fileName, null, null, "실패",
          "파일명과 고지서 모두에서 차량번호를 찾지 못했습니다. 파일명을 차량번호로 바꿔 다시 올려 주세요.", n, loginId,
          null, null, null, null);
    }
    if (filePlate != null && notePlate != null && !filePlate.equals(notePlate)) {
      issues.add("고지서 차량번호(" + notePlate + ")가 파일명(" + filePlate + ")과 다름");
    }

    LocalDateTime at = n == null ? null : parseAt(n.violationAt());
    boolean dateKnown = at != null;
    if (!dateKnown) {
      at = LocalDateTime.now().withSecond(0).withNano(0);
      issues.add("위반일시를 읽지 못해 업로드 시각으로 임시 등록");
    }
    List<ContractMatch> matches = dateKnown ? fineService.findContracts(vehicleNo, at.toLocalDate()) : List.of();
    if (dateKnown && matches.isEmpty()) issues.add("위반일에 해당하는 계약을 찾지 못함");
    else if (matches.size() > 1) issues.add("위반일에 계약 후보가 " + matches.size() + "건 — 맞는 계약을 선택해 주세요");

    String status = issues.isEmpty() ? "접수" : "확인필요";
    StringBuilder remarks = new StringBuilder("고지서 자동 인식 업로드(" + fileName + ")");
    if (n != null && n.note() != null && !n.note().isBlank()) remarks.append(" · 인식 메모: ").append(n.note());
    if (!issues.isEmpty()) remarks.append(" · 확인필요: ").append(String.join(", ", issues));

    try {
      Response created = fineService.create(new SaveRequest(
          vehicleNo, at,
          n == null ? null : blankToNull(n.violationType()),
          n == null ? null : blankToNull(n.violationPlace()),
          n == null ? null : blankToNull(n.issuingAgency()),
          n == null || n.amount() == null || n.amount() <= 0 ? null : n.amount(),
          n == null ? null : parseDate(n.dueDate()),
          null, status, truncate(remarks.toString(), 1000)), loginId);
      if (created.vehicleMgmtNo() == null) issues.add("ERP에 등록되지 않은 차량");
      documentService.upload("과태료", created.vehicleMgmtNo(), vehicleNo, null, null, created.id(), file, loginId);
      return log(batchId, fileName, vehicleNo, created.id(), issues.isEmpty() ? "등록" : "확인필요",
          issues.isEmpty() ? "정상 등록" : String.join(", ", issues), n, loginId,
          created.violationAt(), created.amount(), created.contractNumber(), created.customerName());
    } catch (Exception e) {
      log.warn("[과태료 일괄업로드] {} 실패", fileName, e);
      return log(batchId, fileName, vehicleNo, null, "실패", "등록 실패: " + rootMessage(e), n, loginId,
          null, null, null, null);
    }
  }

  private UploadResult log(String batchId, String fileName, String vehicleNo, Long fineId, String status, String message,
                           FineNoticeReader.Notice n, String loginId, LocalDateTime at, Long amount,
                           String contractNumber, String customerName) {
    String json = null;
    try {
      json = n == null ? null : objectMapper.writeValueAsString(n);
    } catch (Exception ignored) {
      // 기록용 원문 — 직렬화 실패는 무시
    }
    TrafficFineUpload u = uploadRepo.save(TrafficFineUpload.builder()
        .batchId(batchId).fileName(truncate(fileName, 255)).vehicleNo(vehicleNo).fineId(fineId)
        .status(status).message(truncate(message, 1000)).recognizedJson(json)
        .uploadedBy(loginId).uploadedAt(LocalDateTime.now()).build());
    return new UploadResult(u.getId(), fileName, vehicleNo, fineId, status, message, at, amount,
        n == null ? null : blankToNull(n.issuingAgency()), contractNumber, customerName);
  }

  public List<UploadLog> uploads(int limit) {
    return uploadRepo.findAllByOrderByUploadedAtDescIdDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 2000))))
        .stream()
        .map(u -> new UploadLog(u.getId(), u.getBatchId(), u.getFileName(), u.getVehicleNo(), u.getFineId(),
            u.getStatus(), u.getMessage(), u.getUploadedBy(), u.getUploadedAt()))
        .toList();
  }

  // ───────────────────────── 팩스 ─────────────────────────

  /** 팩스로 보낼 문서: 고지서 스캔 + 계약서 스캔 + 임차인 정보 1장 */
  public byte[] faxPdf(Long fineId) throws IOException {
    TrafficFine f = fine(fineId);
    List<VehicleDocument> fineScans = documentService.fineScans(fineId);
    if (fineScans.isEmpty()) throw new IllegalArgumentException("과태료 고지서 스캔이 없습니다.");
    List<PdfMerger.Part> parts = new ArrayList<>();
    for (VehicleDocument d : fineScans) parts.add(part(d));
    if (f.getContractNumber() != null) {
      for (VehicleDocument d : documentService.contractScans(f.getContractNumber())) parts.add(part(d));
      parts.add(new PdfMerger.Part(customerPage(f), "pdf"));
    }
    return PdfMerger.merge(parts);
  }

  public FaxLog sendFax(Long fineId, FaxRequest req, String loginId) throws IOException {
    if (req == null || isBlank(req.receiverName())) throw new IllegalArgumentException("받는사람을 입력해 주세요.");
    String faxNo = req.receiverFax() == null ? "" : req.receiverFax().replaceAll("[^0-9]", "");
    if (faxNo.length() < 9 || faxNo.length() > 12) throw new IllegalArgumentException("받는사람 팩스번호를 확인해 주세요.");
    if (!faxSender.configured()) throw new IllegalStateException("팩스 연동키가 서버에 설정되지 않았습니다. 관리자에게 문의하세요.");
    FaxSetting s = settingRepo.findFirstByOrderByIdAsc().orElse(null);
    if (s == null || isBlank(s.getCorpNum()) || isBlank(s.getSenderNum())) {
      throw new IllegalArgumentException("팩스 발신 설정(사업자번호·발신번호)을 먼저 저장해 주세요.");
    }
    TrafficFine f = fine(fineId);
    if (f.getContractNumber() == null) throw new IllegalArgumentException("계약이 연결되지 않은 과태료입니다. 계약을 먼저 연결해 주세요.");

    byte[] pdf = faxPdf(fineId);
    int pages;
    try (PDDocument doc = PDDocument.load(pdf)) {
      pages = doc.getNumberOfPages();
    }

    TrafficFineFax row = TrafficFineFax.builder()
        .fineId(fineId).vehicleNo(f.getVehicleNo()).contractNumber(f.getContractNumber())
        .customerName(f.getCustomerName()).receiverName(req.receiverName().trim()).receiverFax(faxNo)
        .pageCount(pages).requestedBy(loginId).requestedAt(LocalDateTime.now()).state("접수").build();

    File tmp = Files.createTempFile("fine-fax-" + fineId + "-", ".pdf").toFile();
    try {
      Files.write(tmp.toPath(), pdf);
      String title = "과태료 임차인 변경부과 신청 " + f.getVehicleNo();
      row.setReceiptNum(faxSender.send(s.getCorpNum(), s.getPopbillUserId(), s.getSenderNum(), faxNo,
          row.getReceiverName(), tmp, title));
      row.setResultMessage("팝빌 접수 완료");
      f.setStatus("변경요청");
      fineRepo.save(f);
    } catch (PopbillException e) {
      row.setState("오류");
      row.setResultMessage(truncate("[" + e.getCode() + "] " + e.getMessage(), 500));
    } finally {
      Files.deleteIfExists(tmp.toPath());
    }
    return toLog(faxRepo.save(row));
  }

  public List<FaxLog> faxesOf(Long fineId) {
    return refreshAndMap(faxRepo.findByFineIdOrderByRequestedAtDescIdDesc(fineId));
  }

  public List<FaxLog> faxes(LocalDate from, LocalDate to) {
    LocalDate start = from != null ? from : LocalDate.now().minusDays(30);
    LocalDate end = to != null ? to : LocalDate.now();
    return refreshAndMap(faxRepo.findByRequestedAtBetweenOrderByRequestedAtDescIdDesc(
        start.atStartOfDay(), end.plusDays(1).atStartOfDay()));
  }

  /** 아직 결과가 안 온 팩스는 팝빌에서 상태를 다시 받아 기록을 갱신한다. */
  private List<FaxLog> refreshAndMap(List<TrafficFineFax> rows) {
    FaxSetting s = settingRepo.findFirstByOrderByIdAsc().orElse(null);
    for (TrafficFineFax r : rows) {
      if (s == null || r.getReceiptNum() == null || !PENDING.contains(r.getState()) || !faxSender.configured()) continue;
      try {
        FaxResult[] res = faxSender.result(s.getCorpNum(), r.getReceiptNum(), s.getPopbillUserId());
        if (res == null || res.length == 0) continue;
        FaxResult x = res[0];
        int state = x.getState() == null ? 0 : x.getState();
        if (state == 2) {
          r.setState("전송중");
        } else if (state == 3) {
          boolean ok = nz(x.getFailPageCnt()) == 0 && nz(x.getSuccessPageCnt()) > 0;
          r.setState(ok ? "성공" : "실패");
          r.setResultMessage("성공 " + nz(x.getSuccessPageCnt()) + "장 / 실패 " + nz(x.getFailPageCnt())
              + "장 (결과코드 " + x.getResult() + ")");
        } else if (state == 4) {
          r.setState("실패");
          r.setResultMessage("전송 취소됨");
        }
        if (x.getSendPageCnt() != null) r.setPageCount(x.getSendPageCnt());
        r.setSentAt(parsePopbill(x.getSendDT()));
        r.setResultAt(parsePopbill(x.getResultDT()));
        faxRepo.save(r);
      } catch (Exception e) {
        log.warn("[팩스 결과조회] receiptNum={} 실패: {}", r.getReceiptNum(), e.getMessage());
      }
    }
    return rows.stream().map(this::toLog).toList();
  }

  public FaxSettings settings() {
    FaxSetting s = settingRepo.findFirstByOrderByIdAsc().orElse(new FaxSetting());
    return new FaxSettings(s.getCorpNum(), s.getSenderNum(), s.getSenderName(), s.getPopbillUserId(),
        faxSender.configured());
  }

  public FaxSettings saveSettings(FaxSettings req) {
    String corp = req.corpNum() == null ? "" : req.corpNum().replaceAll("[^0-9]", "");
    if (!corp.isEmpty() && corp.length() != 10) throw new IllegalArgumentException("사업자번호는 10자리입니다.");
    FaxSetting s = settingRepo.findFirstByOrderByIdAsc().orElse(new FaxSetting());
    s.setCorpNum(blankToNull(corp));
    s.setSenderNum(req.senderNum() == null ? null : blankToNull(req.senderNum().replaceAll("[^0-9]", "")));
    s.setSenderName(blankToNull(req.senderName()));
    s.setPopbillUserId(blankToNull(req.popbillUserId()));
    settingRepo.save(s);
    return settings();
  }

  // ───────────────────────── 내부 ─────────────────────────

  private byte[] customerPage(TrafficFine f) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
        SELECT c.contract_number, c.vehicle_no, c.vehicle_model, c.start_date, c.end_date,
               cu.customer_name, cu.customer_type, cu.registration_number, cu.phone, cu.address, cu.ceo
        FROM contracts c LEFT JOIN customers cu ON cu.id = c.customer_id
        WHERE c.contract_number = ?
        """, f.getContractNumber());
    Map<String, Object> c = rows.isEmpty() ? Map.of() : rows.get(0);
    StringBuilder t = new StringBuilder();
    row(t, "차량번호", f.getVehicleNo());
    row(t, "위반일시", f.getViolationAt() == null ? "" : f.getViolationAt().format(AT));
    row(t, "부과기관", f.getIssuingAgency());
    row(t, "위반내용", f.getViolationType());
    row(t, "위반장소", f.getViolationPlace());
    StringBuilder u = new StringBuilder();
    row(u, "임차인", str(c.get("customer_name")));
    row(u, "구분", str(c.get("customer_type")));
    row(u, "주민/사업자등록번호", str(c.get("registration_number")));
    row(u, "대표자", str(c.get("ceo")));
    row(u, "연락처", str(c.get("phone")));
    row(u, "주소", str(c.get("address")));
    row(u, "계약번호", str(c.get("contract_number")));
    row(u, "임대기간", str(c.get("start_date")) + " ~ " + str(c.get("end_date")));
    row(u, "차종", str(c.get("vehicle_model")));
    String xhtml = """
        <html><head><style>
          body { font-family: 'NanumGothic'; font-size: 12pt; }
          h1 { font-size: 18pt; text-align: center; margin-bottom: 18pt; }
          h2 { font-size: 13pt; margin: 16pt 0 6pt; }
          table { width: 100%%; border-collapse: collapse; }
          th, td { border: 1px solid #444; padding: 6pt 8pt; text-align: left; }
          th { width: 32%%; background: #f2f2f2; }
          p.note { margin-top: 18pt; font-size: 10pt; }
        </style></head><body>
          <h1>과태료 임차인(운전자) 정보</h1>
          <h2>위반 내역</h2><table>%s</table>
          <h2>임차인 정보</h2><table>%s</table>
          <p class="note">위 차량은 위반일시 당시 상기 임차인에게 대여 중이었으므로 임차인으로 변경부과를 요청합니다. (임대차계약서 첨부)</p>
        </body></html>
        """.formatted(t, u);
    return pdfRenderer.render(xhtml);
  }

  private static void row(StringBuilder sb, String label, String value) {
    sb.append("<tr><th>").append(PdfRenderer.esc(label)).append("</th><td>")
        .append(PdfRenderer.esc(value == null ? "" : value)).append("</td></tr>");
  }

  private PdfMerger.Part part(VehicleDocument d) {
    return new PdfMerger.Part(documentService.bytesOf(d), VehicleDocumentService.extractExtension(d.getFileName()));
  }

  private TrafficFine fine(Long id) {
    return fineRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("과태료를 찾을 수 없습니다. id=" + id));
  }

  private FaxLog toLog(TrafficFineFax r) {
    return new FaxLog(r.getId(), r.getFineId(), r.getVehicleNo(), r.getContractNumber(), r.getCustomerName(),
        r.getReceiverName(), r.getReceiverFax(), r.getState(), r.getResultMessage(), r.getPageCount(),
        r.getReceiptNum(), r.getRequestedBy(), r.getRequestedAt(), r.getSentAt(), r.getResultAt());
  }

  static String plateOf(String fileName) {
    if (fileName == null) return null;
    String base = fileName.replaceAll("\\.[^.]+$", "").replaceAll("[\\s\\-]", "");
    Matcher m = PLATE.matcher(base);
    return m.find() ? m.group(1) : null;
  }

  private static String normPlate(String s) {
    if (s == null) return null;
    Matcher m = PLATE.matcher(s.replaceAll("[\\s\\-]", ""));
    return m.find() ? m.group(1) : null;
  }

  private static LocalDateTime parseAt(String s) {
    if (isBlank(s)) return null;
    try {
      return LocalDateTime.parse(s.trim(), AT);
    } catch (Exception e) {
      LocalDate d = parseDate(s);
      return d == null ? null : d.atStartOfDay();
    }
  }

  private static LocalDate parseDate(String s) {
    if (isBlank(s)) return null;
    String t = s.trim();
    try {
      return LocalDate.parse(t.substring(0, Math.min(10, t.length())).replace('.', '-').replace('/', '-'));
    } catch (Exception e) {
      return null;
    }
  }

  private static LocalDateTime parsePopbill(String s) {
    if (isBlank(s) || s.length() < 14) return null;
    try {
      return LocalDateTime.parse(s.substring(0, 14), POPBILL_DT);
    } catch (Exception e) {
      return null;
    }
  }

  private static int nz(Integer i) {
    return i == null ? 0 : i;
  }

  private static String str(Object o) {
    return o == null ? "" : o.toString();
  }

  private static String rootMessage(Throwable e) {
    Throwable t = e;
    while (t.getCause() != null && t.getCause() != t) t = t.getCause();
    return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
  }

  private static String truncate(String s, int max) {
    return s == null || s.length() <= max ? s : s.substring(0, max);
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static String blankToNull(String s) {
    return isBlank(s) ? null : s.trim();
  }
}
