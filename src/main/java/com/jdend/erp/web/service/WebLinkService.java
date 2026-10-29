package com.jdend.erp.web.service;

import com.jdend.erp.auth.exception.NotFoundException;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import com.jdend.erp.vehicle.sale.entity.VehicleSale;
import com.jdend.erp.vehicle.sale.repository.VehicleSaleRepository;
import com.jdend.erp.vehicle.service.VehicleOrderService;
import com.jdend.erp.web.dto.WebLinkDtos.*;
import com.jdend.erp.web.entity.WebRentalHold;
import com.jdend.erp.web.repository.WebRentalHoldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 홈페이지(에이스렌터카 단기렌트) 연동.
 * - 가용 차량: 단기 사용 표시 + 등록(차량번호 있음) + 미매각 + 기간 [from, to) 동안
 *   진행 중 계약·가예약/확정 건과 겹치지 않는 차량.
 * - 계약은 LocalDate 라서 시작일 00:00 ~ 종료일 23:59 를 점유한다고 본다.
 * 테넌트(회사 DB)는 TenantFilter 가 API 키로 정해 둔 상태에서 호출된다.
 */
@Service
@RequiredArgsConstructor
public class WebLinkService {

  /** 이 상태의 계약은 차량을 잡고 있지 않다 (ContractRepository.existsByVehicleNoNormalized 와 동일 목록) */
  static final Set<String> TERMINATED_CONTRACT = Set.of(
      "종료", "만기종료", "해지", "중도해지", "중도상환", "만기상환", "완료", "종결", "해지대기"
  );

  /** 이 상태의 가예약은 차량을 잡고 있다 */
  static final Set<String> ACTIVE_HOLD = Set.of(WebRentalHold.STATUS_HOLD, WebRentalHold.STATUS_CONFIRMED);

  /** 한 번에 잡을 수 있는 최대 기간(일) — 잘못된 요청 방어 */
  private static final long MAX_PERIOD_DAYS = 366;

  private static final DateTimeFormatter IN_FMT =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm").withResolverStyle(ResolverStyle.STRICT);
  private static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  private final VehicleOrderRepository orderRepo;
  private final VehicleSaleRepository saleRepo;
  private final ContractRepository contractRepo;
  private final WebRentalHoldRepository holdRepo;

  // ───────────────────────── 공개 API ─────────────────────────

  @Transactional(readOnly = true)
  public AvailabilityResponse availability(String fromStr, String toStr) {
    LocalDateTime from = parseDateTime(fromStr, "from");
    LocalDateTime to = parseDateTime(toStr, "to");
    validatePeriod(from, to);

    List<VehicleOrder> free = filterFree(orderRepo.findShortTermCandidates(), from, to);
    List<AvailableVehicle> out = free.stream()
        .map(v -> AvailableVehicle.builder()
            .id(String.valueOf(v.getId()))
            .plate(v.getVehicleNo())
            .modelCode(modelCodeOf(v))
            .modelName(v.getCarModel())
            .year(parseYear(v.getModelYear()))
            .build())
        .toList();
    return new AvailabilityResponse(out);
  }

  /**
   * 해당 차종코드 차량을 행 잠금(FOR UPDATE)으로 읽은 뒤 기간이 빈 첫 차량을 가예약으로 잡는다.
   * 같은 차종 요청이 동시에 와도 잠금 때문에 차례로 처리되어 같은 차가 두 번 배정되지 않는다.
   * READ_COMMITTED: MySQL 기본(REPEATABLE READ)이면 잠금을 기다린 뒤에도 앞서 커밋된 가예약이
   * 스냅샷에 안 보여 중복 배정될 수 있으므로, 문장마다 최신 커밋을 읽게 한다.
   */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public HoldResponse createHold(HoldRequest req) {
    if (req == null) throw new IllegalArgumentException("요청 본문이 없습니다.");
    String no = required(req.getNo(), "no", 40);
    String code = required(req.getModelCode(), "model_code", 40);
    LocalDateTime pickup = parseDateTime(req.getPickupAt(), "pickup_at");
    LocalDateTime ret = parseDateTime(req.getReturnAt(), "return_at");
    validatePeriod(pickup, ret);
    String location = optional(req.getLocation(), "location", 100);
    String name = optional(req.getName(), "name", 50);
    String phone = optional(req.getPhone(), "phone", 30);

    // 차종 차량 행 잠금을 먼저 잡는다 (같은 차종 요청은 여기서 줄 선다)
    List<VehicleOrder> candidates = orderRepo.findShortTermCandidatesByCodeForUpdate(code);

    // 같은 예약번호 재전송 → 기존 건 그대로 응답 (멱등)
    Optional<WebRentalHold> existing = holdRepo.findByHoldNo(no);
    if (existing.isPresent()) return toHoldResponse(existing.get());

    List<VehicleOrder> free = filterFree(candidates, pickup, ret);
    if (free.isEmpty()) {
      throw new HoldConflictException("해당 기간에 배정 가능한 차량이 없습니다.");
    }
    VehicleOrder v = free.get(0);

    WebRentalHold h = WebRentalHold.builder()
        .holdNo(no)
        .vehicleOrder(v)
        .vehicleNo(v.getVehicleNo())
        .modelCode(code)
        .pickupAt(pickup)
        .returnAt(ret)
        .location(location)
        .customerName(name)
        .phone(phone)
        .status(WebRentalHold.STATUS_HOLD)
        .build();
    holdRepo.save(h);
    return toHoldResponse(h);
  }

  /** 같은 예약번호가 동시에 두 번 들어와 유니크 제약에 걸렸을 때, 먼저 저장된 건을 돌려주기 위함 */
  @Transactional(readOnly = true)
  public Optional<HoldResponse> findHold(String no) {
    if (no == null || no.isBlank()) return Optional.empty();
    return holdRepo.findByHoldNo(no.trim()).map(this::toHoldResponse);
  }

  @Transactional(readOnly = true)
  public VehicleSearchResponse searchVehicles(String q) {
    String kw = q == null ? "" : q.trim().toLowerCase();
    String kwPlate = normalize(kw);
    if (kw.length() > 50) throw new IllegalArgumentException("검색어가 너무 깁니다.");
    SoldIndex sold = soldIndex();

    List<VehicleInfo> out = orderRepo.findAll().stream()
        .filter(v -> isRegistered(v) && !sold.isSold(v))
        .filter(v -> kw.isEmpty()
            || normalize(v.getVehicleNo()).toLowerCase().contains(kwPlate)
            || (v.getCarModel() != null && v.getCarModel().toLowerCase().contains(kw)))
        .sorted(Comparator.comparing(VehicleOrder::getId).reversed())
        .limit(30)
        .map(v -> VehicleInfo.builder()
            .id(String.valueOf(v.getId()))
            .plate(v.getVehicleNo())
            .name(v.getCarModel())
            .vehicleClass(null)
            .year(parseYear(v.getModelYear()))
            .firstReg(v.getFirstRegDate() == null ? null
                : v.getFirstRegDate().getYear() + "년 " + v.getFirstRegDate().getMonthValue() + "월")
            .km(null)
            .fuel(v.getFuelType())
            .color(null)
            .seats(null)
            .options(v.getOptionName())
            .build())
        .toList();
    return new VehicleSearchResponse(out);
  }

  // ───────────────────────── ERP 화면 ─────────────────────────

  @Transactional(readOnly = true)
  public List<ShortVehicleRow> listVehicles(String keyword) {
    String kw = keyword == null ? "" : keyword.trim().toLowerCase();
    String kwPlate = normalize(kw);
    SoldIndex sold = soldIndex();
    return orderRepo.findAll().stream()
        .filter(v -> isRegistered(v) && !sold.isSold(v))
        .filter(v -> kw.isEmpty()
            || normalize(v.getVehicleNo()).toLowerCase().contains(kwPlate)
            || (v.getCarModel() != null && v.getCarModel().toLowerCase().contains(kw)))
        .sorted(Comparator.comparing((VehicleOrder v) -> !Boolean.TRUE.equals(v.getShortTermUse()))
            .thenComparing(v -> v.getCarModel() == null ? "" : v.getCarModel())
            .thenComparing(VehicleOrder::getVehicleNo))
        .map(this::toVehicleRow)
        .toList();
  }

  @Transactional
  public ShortVehicleRow updateVehicle(Long id, ShortVehicleUpdateRequest req) {
    VehicleOrder v = orderRepo.findById(id).orElseThrow(() -> new NotFoundException("차량 없음(id): " + id));
    if (req.getShortTermUse() != null) v.setShortTermUse(req.getShortTermUse());
    if (req.getWebModelCode() != null) {
      String code = req.getWebModelCode().trim();
      if (code.length() > 40) throw new IllegalArgumentException("홈페이지 차종코드는 40자 이내로 입력하세요.");
      v.setWebModelCode(code.isEmpty() ? null : code);
    }
    orderRepo.save(v);
    return toVehicleRow(v);
  }

  @Transactional(readOnly = true)
  public List<HoldRow> listHolds(String status, boolean includePast) {
    String st = status == null ? "" : status.trim();
    LocalDateTime fromAt = includePast ? null : LocalDate.now().atStartOfDay();
    return holdRepo.search(st, fromAt).stream().map(this::toHoldRow).toList();
  }

  /** 가예약 → 확정/취소, 확정 → 취소. 취소된 건은 되살리지 않는다(홈페이지에서 새로 받는다). */
  @Transactional
  public HoldRow changeHoldStatus(Long id, String status) {
    WebRentalHold h = holdRepo.findById(id).orElseThrow(() -> new NotFoundException("가예약 없음(id): " + id));
    String next = status == null ? "" : status.trim();
    if (!WebRentalHold.STATUS_CONFIRMED.equals(next) && !WebRentalHold.STATUS_CANCELED.equals(next)) {
      throw new IllegalArgumentException("상태는 확정 또는 취소만 가능합니다.");
    }
    String cur = h.getStatus();
    if (WebRentalHold.STATUS_CANCELED.equals(cur)) {
      throw new IllegalArgumentException("이미 취소된 가예약입니다.");
    }
    if (WebRentalHold.STATUS_CONFIRMED.equals(cur) && WebRentalHold.STATUS_CONFIRMED.equals(next)) {
      throw new IllegalArgumentException("이미 확정된 가예약입니다.");
    }
    h.setStatus(next);
    holdRepo.save(h);
    return toHoldRow(h);
  }

  // ───────────────────────── 내부 ─────────────────────────

  /** 후보 차량 중 기간 [from, to) 동안 매각·계약·가예약과 겹치지 않는 차량만 (입력 순서 유지) */
  private List<VehicleOrder> filterFree(List<VehicleOrder> candidates, LocalDateTime from, LocalDateTime to) {
    if (candidates.isEmpty()) return List.of();
    SoldIndex sold = soldIndex();

    // 계약: 시작일 00:00 ~ 종료일 다음날 00:00 점유. [from, to) 와 겹침 ⇔ start <= (to-1분).date && end >= from.date
    LocalDate fromDate = from.toLocalDate();
    LocalDate toDate = to.minusMinutes(1).toLocalDate();
    Set<String> contracted = contractRepo.findActiveVehicleNosOverlapping(fromDate, toDate, TERMINATED_CONTRACT)
        .stream().map(WebLinkService::normalize).filter(s -> !s.isEmpty()).collect(Collectors.toSet());

    Set<Long> held = new HashSet<>(holdRepo.findBusyVehicleIds(from, to, ACTIVE_HOLD));

    return candidates.stream()
        .filter(this::isRegistered)
        .filter(v -> !sold.isSold(v))
        .filter(v -> !contracted.contains(normalize(v.getVehicleNo())))
        .filter(v -> !held.contains(v.getId()))
        .toList();
  }

  private boolean isRegistered(VehicleOrder v) {
    return v.getVehicleNo() != null && !v.getVehicleNo().isBlank();
  }

  /** 매각: orderStatus='매각' 이거나 vehicle_sales 에 (차량 id 또는 차량관리번호로) 있음 */
  private SoldIndex soldIndex() {
    List<VehicleSale> sales = saleRepo.findAll();
    Set<Long> ids = new HashSet<>();
    Set<String> mgmt = new HashSet<>();
    for (VehicleSale s : sales) {
      if (s.getVehicleOrderId() != null) ids.add(s.getVehicleOrderId());
      if (s.getVehicleMgmtNo() != null && !s.getVehicleMgmtNo().isBlank()) mgmt.add(s.getVehicleMgmtNo().trim());
    }
    return new SoldIndex(ids, mgmt);
  }

  private record SoldIndex(Set<Long> ids, Set<String> mgmtNos) {
    boolean isSold(VehicleOrder v) {
      return VehicleOrderService.SOLD_STATUS.equals(v.getOrderStatus())
          || ids.contains(v.getId())
          || (v.getVehicleMgmtNo() != null && mgmtNos.contains(v.getVehicleMgmtNo().trim()));
    }
  }

  static String modelCodeOf(VehicleOrder v) {
    String c = v.getWebModelCode();
    return (c == null || c.isBlank()) ? v.getCarModel() : c.trim();
  }

  private ShortVehicleRow toVehicleRow(VehicleOrder v) {
    return ShortVehicleRow.builder()
        .id(v.getId())
        .vehicleNo(v.getVehicleNo())
        .carModel(v.getCarModel())
        .modelYear(v.getModelYear())
        .orderStatus(v.getOrderStatus())
        .operationType(v.getOperationType())
        .shortTermUse(Boolean.TRUE.equals(v.getShortTermUse()))
        .webModelCode(v.getWebModelCode())
        .build();
  }

  private HoldRow toHoldRow(WebRentalHold h) {
    VehicleOrder v = h.getVehicleOrder();
    return HoldRow.builder()
        .id(h.getId())
        .holdNo(h.getHoldNo())
        .vehicleOrderId(v == null ? null : v.getId())
        .vehicleNo(h.getVehicleNo())
        .modelCode(h.getModelCode())
        .carModel(v == null ? null : v.getCarModel())
        .pickupAt(fmt(h.getPickupAt()))
        .returnAt(fmt(h.getReturnAt()))
        .location(h.getLocation())
        .customerName(h.getCustomerName())
        .phone(h.getPhone())
        .status(h.getStatus())
        .createdAt(fmt(h.getCreatedAt()))
        .build();
  }

  private HoldResponse toHoldResponse(WebRentalHold h) {
    return HoldResponse.builder()
        .ok(true)
        .holdId(h.getId())
        .vehicleId(h.getVehicleOrder() == null ? null : String.valueOf(h.getVehicleOrder().getId()))
        .plate(h.getVehicleNo())
        .build();
  }

  private static String fmt(LocalDateTime t) {
    return t == null ? null : t.format(OUT_FMT);
  }

  private static LocalDateTime parseDateTime(String s, String field) {
    if (s == null || s.isBlank()) throw new IllegalArgumentException(field + " 값이 필요합니다. (YYYY-MM-DDTHH:MM)");
    try {
      return LocalDateTime.parse(s.trim(), IN_FMT);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(field + " 형식이 올바르지 않습니다. (YYYY-MM-DDTHH:MM)");
    }
  }

  private static void validatePeriod(LocalDateTime from, LocalDateTime to) {
    if (!to.isAfter(from)) throw new IllegalArgumentException("반납 일시는 대여 일시보다 뒤여야 합니다.");
    if (from.plusDays(MAX_PERIOD_DAYS).isBefore(to)) {
      throw new IllegalArgumentException("기간이 너무 깁니다. (최대 " + MAX_PERIOD_DAYS + "일)");
    }
  }

  private static String required(String s, String field, int max) {
    if (s == null || s.isBlank()) throw new IllegalArgumentException(field + " 값이 필요합니다.");
    String t = s.trim();
    if (t.length() > max) throw new IllegalArgumentException(field + " 값은 " + max + "자 이내여야 합니다.");
    return t;
  }

  private static String optional(String s, String field, int max) {
    if (s == null || s.isBlank()) return null;
    String t = s.trim();
    if (t.length() > max) throw new IllegalArgumentException(field + " 값은 " + max + "자 이내여야 합니다.");
    return t;
  }

  /** "2024", "2024년식", "24MY" 등에서 4자리 연도만 뽑는다. 없으면 null */
  static Integer parseYear(String s) {
    if (s == null) return null;
    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(19|20)\\d{2}").matcher(s);
    return m.find() ? Integer.valueOf(m.group()) : null;
  }

  static String normalize(String s) {
    return s == null ? "" : s.trim().replace(" ", "").replace("-", "");
  }

  /** 배정 가능한 차량 없음 → 409 */
  public static class HoldConflictException extends RuntimeException {
    public HoldConflictException(String message) { super(message); }
  }
}
