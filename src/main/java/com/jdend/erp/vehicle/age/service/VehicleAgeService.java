package com.jdend.erp.vehicle.age.service;

import com.jdend.erp.vehicle.age.dto.VehicleAgeDtos.BulkItem;
import com.jdend.erp.vehicle.age.dto.VehicleAgeDtos.BulkResult;
import com.jdend.erp.vehicle.age.dto.VehicleAgeDtos.Row;
import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import com.jdend.erp.vehicle.service.VehicleOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** 차령관리: 차량 1대당 차령만료일 하나를 vehicle_orders.vehicle_age_expiry_date 에 둔다. */
@Service
@RequiredArgsConstructor
public class VehicleAgeService {

  /** 만료임박으로 보는 기간(일) */
  private static final int SOON_DAYS = 90;

  private final VehicleOrderRepository orderRepo;

  @Transactional(readOnly = true)
  public List<Row> list(String keyword, String status, LocalDate expiryFrom, LocalDate expiryTo, boolean includeSold) {
    String kw = keyword == null ? "" : keyword.trim().replace(" ", "").toLowerCase();
    LocalDate today = LocalDate.now();

    return orderRepo.findAll().stream()
        .filter(o -> o.getVehicleNo() != null && !o.getVehicleNo().isBlank())
        .filter(o -> includeSold || !VehicleOrderService.SOLD_STATUS.equals(o.getOrderStatus()))
        .filter(o -> kw.isEmpty()
            || contains(o.getVehicleNo(), kw) || contains(o.getVehicleMgmtNo(), kw) || contains(o.getCarModel(), kw))
        .filter(o -> expiryFrom == null
            || (o.getVehicleAgeExpiryDate() != null && !o.getVehicleAgeExpiryDate().isBefore(expiryFrom)))
        .filter(o -> expiryTo == null
            || (o.getVehicleAgeExpiryDate() != null && !o.getVehicleAgeExpiryDate().isAfter(expiryTo)))
        .map(o -> toRow(o, today))
        .filter(r -> status == null || status.isBlank() || status.equals(r.getStatus()))
        // 만료일 빠른 순, 미등록은 뒤로
        .sorted(Comparator.comparing(Row::getVehicleAgeExpiryDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Row::getVehicleNo))
        .toList();
  }

  @Transactional
  public Row update(Long id, LocalDate expiry) {
    VehicleOrder o = orderRepo.findById(id).orElseThrow(() -> new RuntimeException("차량 없음(id): " + id));
    o.setVehicleAgeExpiryDate(expiry);
    orderRepo.save(o);
    return toRow(o, LocalDate.now());
  }

  /** 차량관리번호(우선) 또는 차량번호로 찾아 저장. 못 찾거나 여러 대면 그 줄만 오류로 남긴다. */
  @Transactional
  public BulkResult bulkUpdate(List<BulkItem> items) {
    List<String> errors = new ArrayList<>();
    int updated = 0, unchanged = 0;
    List<VehicleOrder> all = orderRepo.findAll();

    for (int i = 0; i < items.size(); i++) {
      BulkItem it = items.get(i);
      String label = (i + 1) + "행(" + firstNonBlank(it.getVehicleMgmtNo(), it.getVehicleNo()) + ")";
      List<VehicleOrder> found;
      if (it.getVehicleMgmtNo() != null && !it.getVehicleMgmtNo().isBlank()) {
        String m = it.getVehicleMgmtNo().trim();
        found = all.stream().filter(o -> m.equals(o.getVehicleMgmtNo())).toList();
      } else if (it.getVehicleNo() != null && !it.getVehicleNo().isBlank()) {
        String v = normalize(it.getVehicleNo());
        found = all.stream().filter(o -> v.equals(normalize(o.getVehicleNo()))).toList();
      } else {
        errors.add(label + ": 차량관리번호 또는 차량번호가 없습니다.");
        continue;
      }
      if (found.isEmpty()) {
        errors.add(label + ": 차량을 찾을 수 없습니다.");
        continue;
      }
      if (found.size() > 1) {
        errors.add(label + ": 같은 번호의 차량이 " + found.size() + "대입니다. 차량관리번호로 지정하세요.");
        continue;
      }
      VehicleOrder o = found.get(0);
      if (Objects.equals(o.getVehicleAgeExpiryDate(), it.getVehicleAgeExpiryDate())) {
        unchanged++;
        continue;
      }
      o.setVehicleAgeExpiryDate(it.getVehicleAgeExpiryDate());
      orderRepo.save(o);
      updated++;
    }
    return BulkResult.builder().total(items.size()).updated(updated).unchanged(unchanged).errors(errors).build();
  }

  private Row toRow(VehicleOrder o, LocalDate today) {
    LocalDate exp = o.getVehicleAgeExpiryDate();
    Long dday = exp == null ? null : ChronoUnit.DAYS.between(today, exp);
    String st;
    if (exp == null) st = "미등록";
    else if (dday < 0) st = "만료";
    else if (dday <= SOON_DAYS) st = "만료임박";
    else st = "정상";
    return Row.builder()
        .id(o.getId())
        .vehicleMgmtNo(o.getVehicleMgmtNo())
        .vehicleNo(o.getVehicleNo())
        .carModel(o.getCarModel())
        .orderStatus(o.getOrderStatus())
        .firstRegDate(o.getFirstRegDate())
        .vehicleAgeExpiryDate(exp)
        .dday(dday)
        .status(st)
        .build();
  }

  private static boolean contains(String s, String kw) {
    return s != null && s.replace(" ", "").toLowerCase().contains(kw);
  }

  private static String normalize(String s) {
    return s == null ? "" : s.trim().replace(" ", "").replace("-", "");
  }

  private static String firstNonBlank(String a, String b) {
    return (a != null && !a.isBlank()) ? a.trim() : (b == null ? "" : b.trim());
  }
}
