package com.jdend.erp.vehicle.inspection.service;

import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.inspection.dto.*;
import com.jdend.erp.vehicle.inspection.entity.VehicleInspection;
import com.jdend.erp.vehicle.inspection.repository.VehicleInspectionRepository;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VehicleInspectionService {

  private final VehicleInspectionRepository inspectionRepo;
  private final VehicleOrderRepository orderRepo;

  @Transactional
  public VehicleInspectionResponse create(VehicleInspectionCreateRequest req) {
    if (req == null) throw new RuntimeException("요청값이 비었습니다.");
    if (isBlank(req.vehicleMgmtNo)) throw new RuntimeException("vehicleMgmtNo 필수");
    if (req.validStart == null || req.validEnd == null) throw new RuntimeException("validStart/validEnd 필수");
    if (req.validStart.isAfter(req.validEnd)) throw new RuntimeException("validStart는 validEnd보다 클 수 없습니다.");

    String mgmtNo = req.vehicleMgmtNo.trim();

    VehicleOrder vo = orderRepo.findByVehicleMgmtNo(mgmtNo)
        .orElseThrow(() -> new RuntimeException("차량 없음: " + mgmtNo));

    VehicleInspection saved = inspectionRepo.save(
        VehicleInspection.builder()
            .vehicleOrderId(vo.getId())
            .vehicleMgmtNo(mgmtNo)
            .vehicleNo(vo.getVehicleNo())
            .inspectionDate(req.inspectionDate)
            .validStart(req.validStart)
            .validEnd(req.validEnd)
            .vendor(blankToNull(req.vendor))
            .inspectionPlace(blankToNull(req.inspectionPlace))
            .inspectionCost(req.inspectionCost != null ? req.inspectionCost : 0L)
            .memo(blankToNull(req.memo))
            .build()
    );

    vo.setInspectionStart(req.validStart);
    vo.setInspectionEnd(req.validEnd);
    orderRepo.save(vo);

    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<VehicleInspectionRowResponse> search(
      String vehicleMgmtNo,
      String vehicleNo,
      LocalDate validStartFrom,
      LocalDate validEndTo
  ) {
    Map<String, VehicleInspectionRowResponse> resultMap = new LinkedHashMap<>();

    // 1. vehicle_inspections 테이블에 직접 등록된 검사 데이터 조회
    Specification<VehicleInspection> spec = (root, query, cb) -> {
      List<Predicate> preds = new ArrayList<>();

      if (!isBlank(vehicleMgmtNo)) {
        preds.add(cb.like(root.get("vehicleMgmtNo"), "%" + vehicleMgmtNo.trim() + "%"));
      }
      if (!isBlank(vehicleNo)) {
        preds.add(cb.like(root.get("vehicleNo"), "%" + vehicleNo.trim() + "%"));
      }
      if (validStartFrom != null) {
        preds.add(cb.greaterThanOrEqualTo(root.get("validStart"), validStartFrom));
      }
      if (validEndTo != null) {
        preds.add(cb.lessThanOrEqualTo(root.get("validEnd"), validEndTo));
      }

      return cb.and(preds.toArray(new Predicate[0]));
    };

    List<VehicleInspection> inspections = inspectionRepo.findAll(
        spec,
        Sort.by(Sort.Direction.DESC, "validEnd", "validStart", "id")
    );

    for (VehicleInspection vi : inspections) {
      VehicleOrder order = orderRepo.findByVehicleMgmtNo(vi.getVehicleMgmtNo()).orElse(null);

      String key = vi.getVehicleMgmtNo();

      resultMap.put(key, VehicleInspectionRowResponse.builder()
          .id(vi.getId())
          .vehicleMgmtNo(vi.getVehicleMgmtNo())
          .vehicleNo(vi.getVehicleNo())
          .carModel(order != null ? order.getCarModel() : null)
          .validStart(vi.getValidStart())
          .validEnd(vi.getValidEnd())
          .inspectionDate(vi.getInspectionDate())
          .build());
    }

    // 2. vehicle_orders 테이블에 검사유효기간이 있는 차량도 조회
    List<VehicleOrder> orders = orderRepo.searchInspectionVehicles(
        blankToNull(vehicleMgmtNo),
        blankToNull(vehicleNo),
        validStartFrom,
        validEndTo
    );

    for (VehicleOrder vo : orders) {
      String key = vo.getVehicleMgmtNo();

      // 이미 vehicle_inspections에 등록된 차량이면 중복으로 넣지 않음
      if (resultMap.containsKey(key)) {
        continue;
      }

      resultMap.put(key, VehicleInspectionRowResponse.builder()
          .id(null)
          .vehicleMgmtNo(vo.getVehicleMgmtNo())
          .vehicleNo(vo.getVehicleNo())
          .carModel(vo.getCarModel())
          .validStart(vo.getInspectionStart())
          .validEnd(vo.getInspectionEnd())
          .inspectionDate(null)
          .build());
    }

    List<VehicleInspectionRowResponse> result = new ArrayList<>(resultMap.values());

    result.sort((a, b) -> {
      LocalDate aEnd = a.getValidEnd();
      LocalDate bEnd = b.getValidEnd();

      if (aEnd == null && bEnd == null) return 0;
      if (aEnd == null) return 1;
      if (bEnd == null) return -1;

      return bEnd.compareTo(aEnd);
    });

    return result;
  }

  private VehicleInspectionResponse toResponse(VehicleInspection e) {
    return VehicleInspectionResponse.builder()
        .id(e.getId())
        .vehicleOrderId(e.getVehicleOrderId())
        .vehicleMgmtNo(e.getVehicleMgmtNo())
        .vehicleNo(e.getVehicleNo())
        .inspectionDate(e.getInspectionDate())
        .validStart(e.getValidStart())
        .validEnd(e.getValidEnd())
        .vendor(e.getVendor())
        .inspectionPlace(e.getInspectionPlace())
        .inspectionCost(e.getInspectionCost())
        .memo(e.getMemo())
        .build();
  }

  private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private String blankToNull(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}