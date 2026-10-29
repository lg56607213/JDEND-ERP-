package com.jdend.erp.vehicle.fine.service;

import com.jdend.erp.auth.exception.NotFoundException;
import com.jdend.erp.common.pdf.PdfMerger;
import com.jdend.erp.document.entity.VehicleDocument;
import com.jdend.erp.document.service.VehicleDocumentService;
import com.jdend.erp.vehicle.fine.dto.TrafficFineDtos.ContractMatch;
import com.jdend.erp.vehicle.fine.dto.TrafficFineDtos.Response;
import com.jdend.erp.vehicle.fine.dto.TrafficFineDtos.SaveRequest;
import com.jdend.erp.vehicle.fine.entity.TrafficFine;
import com.jdend.erp.vehicle.fine.repository.TrafficFineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TrafficFineService {

  /** 확인필요: 고지서 자동 인식에서 차량번호·위반일시·계약 연결 중 하나라도 확실하지 않을 때 */
  private static final Set<String> STATUSES = Set.of("접수", "확인필요", "변경요청", "처리완료");

  private final TrafficFineRepository fineRepo;
  private final VehicleDocumentService documentService;
  private final JdbcTemplate jdbcTemplate;

  @Transactional(readOnly = true)
  public List<Response> list(String vehicleNo, String customerName, String status,
                             LocalDate startDate, LocalDate endDate) {
    List<TrafficFine> fines = fineRepo.search(
      blankToNull(vehicleNo), blankToNull(customerName), blankToNull(status),
      startDate == null ? null : startDate.atStartOfDay(),
      endDate == null ? null : endDate.plusDays(1).atStartOfDay());

    Set<Long> scannedFines = documentService.fineIdsWithScan(
      fines.stream().map(TrafficFine::getId).toList());
    List<String> contractNos = fines.stream()
      .map(TrafficFine::getContractNumber).filter(Objects::nonNull).distinct().toList();
    Map<String, Boolean> scannedContracts = documentService.contractScanStatus(contractNos);

    return fines.stream()
      .map(f -> toRes(f, scannedFines.contains(f.getId()),
        Boolean.TRUE.equals(scannedContracts.get(f.getContractNumber()))))
      .toList();
  }

  @Transactional(readOnly = true)
  public Response detail(Long id) {
    TrafficFine f = get(id);
    return toRes(f,
      !documentService.fineScans(id).isEmpty(),
      !documentService.contractScans(f.getContractNumber()).isEmpty());
  }

  @Transactional
  public Response create(SaveRequest req, String loginId) {
    TrafficFine f = new TrafficFine();
    f.setCreatedBy(loginId);
    apply(f, req);
    return detail(fineRepo.save(f).getId());
  }

  @Transactional
  public Response update(Long id, SaveRequest req) {
    TrafficFine f = get(id);
    apply(f, req);
    fineRepo.save(f);
    return detail(id);
  }

  @Transactional
  public void delete(Long id) {
    TrafficFine f = get(id);
    documentService.fineScans(id).forEach(d -> documentService.delete(d.getId()));
    fineRepo.delete(f);
  }

  /**
   * 차량번호와 위반일로 그날 차량을 가지고 있던 계약을 찾는다.
   * 중도해지된 계약은 해지일까지만 유효하다. 반납·인도가 같은 날이면 후보가 둘 이상일 수 있어
   * 시작일이 늦은 순으로 모두 돌려준다.
   */
  @Transactional(readOnly = true)
  public List<ContractMatch> findContracts(String vehicleNo, LocalDate date) {
    if (isBlank(vehicleNo) || date == null) return List.of();
    return jdbcTemplate.query("""
        SELECT c.contract_number, cu.customer_name, c.start_date, c.end_date,
               MAX(et.termination_date) AS termination_date
        FROM contracts c
        LEFT JOIN customers cu ON cu.id = c.customer_id
        LEFT JOIN early_terminations et ON et.contract_id = c.id
        WHERE REPLACE(REPLACE(TRIM(IFNULL(c.vehicle_no,'')), ' ', ''), '-', '')
            = REPLACE(REPLACE(TRIM(?), ' ', ''), '-', '')
          AND c.start_date <= ?
        GROUP BY c.id, c.contract_number, cu.customer_name, c.start_date, c.end_date
        HAVING COALESCE(MAX(et.termination_date), c.end_date) >= ?
        ORDER BY c.start_date DESC, c.id DESC
        """,
      (rs, i) -> new ContractMatch(
        rs.getString("contract_number"),
        rs.getString("customer_name"),
        toLocalDate(rs.getDate("start_date")),
        toLocalDate(rs.getDate("end_date")),
        toLocalDate(rs.getDate("termination_date"))),
      vehicleNo.trim(), Date.valueOf(date), Date.valueOf(date));
  }

  /** 과태료 스캔본 + 계약서 스캔본을 한 PDF로 합친다. */
  @Transactional(readOnly = true)
  public byte[] printPdf(Long id) throws IOException {
    TrafficFine f = get(id);
    List<VehicleDocument> docs = new ArrayList<>(documentService.fineScans(id));
    if (docs.isEmpty()) {
      throw new IllegalArgumentException("과태료 고지서 스캔이 없습니다. 고지서를 먼저 스캔해 주세요.");
    }
    docs.addAll(documentService.contractScans(f.getContractNumber()));

    List<PdfMerger.Part> parts = new ArrayList<>();
    for (VehicleDocument d : docs) {
      parts.add(new PdfMerger.Part(documentService.bytesOf(d),
        VehicleDocumentService.extractExtension(d.getFileName())));
    }
    return PdfMerger.merge(parts);
  }

  // ──────────────────────────────────────────────

  private void apply(TrafficFine f, SaveRequest req) {
    if (req == null) throw new IllegalArgumentException("요청값이 비었습니다.");
    if (isBlank(req.vehicleNo())) throw new IllegalArgumentException("차량번호를 입력해 주세요.");
    if (req.violationAt() == null) throw new IllegalArgumentException("위반일시를 입력해 주세요.");
    if (req.status() != null && !req.status().isBlank() && !STATUSES.contains(req.status())) {
      throw new IllegalArgumentException("알 수 없는 처리상태입니다: " + req.status());
    }

    String vehicleNo = req.vehicleNo().trim();
    f.setVehicleNo(vehicleNo);
    f.setVehicleMgmtNo(vehicleMgmtNoOf(vehicleNo));
    f.setViolationAt(req.violationAt());
    f.setViolationType(blankToNull(req.violationType()));
    f.setViolationPlace(blankToNull(req.violationPlace()));
    f.setIssuingAgency(blankToNull(req.issuingAgency()));
    f.setAmount(req.amount());
    f.setDueDate(req.dueDate());
    if (!isBlank(req.status())) f.setStatus(req.status());
    f.setRemarks(blankToNull(req.remarks()));

    // contractNumber 가 없으면(null) 위반일 기준으로 자동 연결, 빈 문자열이면 "계약 없음"
    List<ContractMatch> matches = findContracts(vehicleNo, req.violationAt().toLocalDate());
    String contractNumber = blankToNull(req.contractNumber());
    if (req.contractNumber() == null && !matches.isEmpty()) {
      contractNumber = matches.get(0).contractNumber();
    }
    f.setContractNumber(contractNumber);
    final String cn = contractNumber;
    f.setCustomerName(cn == null ? null : matches.stream()
      .filter(m -> cn.equals(m.contractNumber())).map(ContractMatch::customerName)
      .findFirst().orElseGet(() -> customerNameOf(cn)));
  }

  /** 같은 번호판이 여러 건(재등록 등)이면 가장 최근 차량의 관리번호를 쓴다. */
  private String vehicleMgmtNoOf(String vehicleNo) {
    List<String> nos = jdbcTemplate.queryForList("""
        SELECT vehicle_mgmt_no FROM vehicle_orders
        WHERE REPLACE(REPLACE(TRIM(IFNULL(vehicle_no,'')), ' ', ''), '-', '')
            = REPLACE(REPLACE(TRIM(?), ' ', ''), '-', '')
        ORDER BY id DESC LIMIT 1
        """, String.class, vehicleNo);
    return nos.isEmpty() ? null : nos.get(0);
  }

  private String customerNameOf(String contractNumber) {
    List<String> names = jdbcTemplate.queryForList("""
        SELECT cu.customer_name FROM contracts c
        LEFT JOIN customers cu ON cu.id = c.customer_id
        WHERE c.contract_number = ?
        """, String.class, contractNumber);
    if (names.isEmpty()) {
      throw new IllegalArgumentException("계약번호를 찾을 수 없습니다: " + contractNumber);
    }
    return names.get(0);
  }

  private TrafficFine get(Long id) {
    return fineRepo.findById(id).orElseThrow(() -> new NotFoundException("과태료를 찾을 수 없습니다. id=" + id));
  }

  private Response toRes(TrafficFine f, boolean fineScanned, boolean contractScanned) {
    return new Response(
      f.getId(), f.getVehicleNo(), f.getVehicleMgmtNo(), f.getViolationAt(),
      f.getViolationType(), f.getViolationPlace(), f.getIssuingAgency(), f.getAmount(),
      f.getDueDate(), f.getContractNumber(), f.getCustomerName(), f.getStatus(),
      f.getRemarks(), fineScanned, contractScanned, f.getCreatedAt());
  }

  private static LocalDate toLocalDate(Date d) {
    return d == null ? null : d.toLocalDate();
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static String blankToNull(String s) {
    return isBlank(s) ? null : s.trim();
  }
}
