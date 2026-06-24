package com.jdend.erp.vehicle.dispatch.service;

import com.jdend.erp.vehicle.dispatch.dto.*;
import com.jdend.erp.vehicle.dispatch.entity.VehicleDispatch;
import com.jdend.erp.vehicle.dispatch.repository.VehicleDispatchRepository;
import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleDispatchService {

  private final VehicleDispatchRepository dispatchRepo;
  private final VehicleOrderRepository vehicleOrderRepo;
  private final JdbcTemplate jdbcTemplate;

  @Transactional
  public DispatchResponse create(DispatchCreateRequest req) {
    if (req == null) throw new RuntimeException("요청값이 비었습니다.");
    if (isBlank(req.vehicleMgmtNo)) throw new RuntimeException("vehicleMgmtNo 필수");
    if (isBlank(req.dispatchType)) throw new RuntimeException("dispatchType 필수");
    if (req.dispatchDate == null) throw new RuntimeException("dispatchDate 필수");

    if (isBlank(req.departureAddress)) throw new RuntimeException("departureAddress 필수");
    if (isBlank(req.departureContact)) throw new RuntimeException("departureContact 필수");
    if (isBlank(req.arrivalAddress)) throw new RuntimeException("arrivalAddress 필수");
    if (isBlank(req.arrivalContact)) throw new RuntimeException("arrivalContact 필수");

    VehicleOrder vo = vehicleOrderRepo.findByVehicleMgmtNo(req.vehicleMgmtNo.trim())
      .orElseThrow(() -> new RuntimeException("차량 없음: " + req.vehicleMgmtNo));

    String vehicleNo = firstNonBlank(req.vehicleNo, vo.getVehicleNo());
    String contractNumber = findLatestContractNumberByVehicleNo(vehicleNo);

    VehicleDispatch d = VehicleDispatch.builder()
      .vehicleOrder(vo)
      .vehicleMgmtNo(vo.getVehicleMgmtNo())
      .vehicleNo(emptyToNull(vehicleNo))
      .contractNumber(emptyToNull(contractNumber))

      .dispatchType(req.dispatchType.trim())
      .dispatchDate(req.dispatchDate)

      .departureAddress(req.departureAddress.trim())
      .departureContact(req.departureContact.trim())
      .arrivalAddress(req.arrivalAddress.trim())
      .arrivalContact(req.arrivalContact.trim())

      .remarks(emptyToNull(req.remarks))
      .build();

    VehicleDispatch saved = dispatchRepo.save(d);
    return toRes(saved);
  }

  @Transactional(readOnly = true)
  public List<DispatchResponse> list(String contractNumber, String vehicleNo, LocalDate startDate, LocalDate endDate) {
    return dispatchRepo.search(
      emptyToNull(contractNumber),
      emptyToNull(vehicleNo),
      startDate,
      endDate
    ).stream().map(this::toRes).toList();
  }

  @Transactional(readOnly = true)
  public DispatchResponse detail(Long id) {
    VehicleDispatch d = dispatchRepo.findById(id)
      .orElseThrow(() -> new RuntimeException("배차 없음: " + id));
    return toRes(d);
  }

  @Transactional
  public DispatchResponse update(Long id, DispatchUpdateRequest req) {
    if (req == null) throw new RuntimeException("요청값이 비었습니다.");
    if (isBlank(req.dispatchType)) throw new RuntimeException("dispatchType 필수");
    if (req.dispatchDate == null) throw new RuntimeException("dispatchDate 필수");

    if (isBlank(req.departureAddress)) throw new RuntimeException("departureAddress 필수");
    if (isBlank(req.departureContact)) throw new RuntimeException("departureContact 필수");
    if (isBlank(req.arrivalAddress)) throw new RuntimeException("arrivalAddress 필수");
    if (isBlank(req.arrivalContact)) throw new RuntimeException("arrivalContact 필수");

    VehicleDispatch d = dispatchRepo.findById(id)
      .orElseThrow(() -> new RuntimeException("배차 없음: " + id));

    d.setDispatchDate(req.dispatchDate);
    d.setDispatchType(req.dispatchType.trim());

    d.setDepartureAddress(req.departureAddress.trim());
    d.setDepartureContact(req.departureContact.trim());
    d.setArrivalAddress(req.arrivalAddress.trim());
    d.setArrivalContact(req.arrivalContact.trim());

    d.setRemarks(emptyToNull(req.remarks));

    dispatchRepo.save(d);
    return toRes(d);
  }

  // ✅ 차량번호로 최신 계약번호 1건 조회
  private String findLatestContractNumberByVehicleNo(String vehicleNo) {
    if (isBlank(vehicleNo)) return null;

    String sql = """
      select contract_number
      from contracts
      where replace(replace(trim(vehicle_no), ' ', ''), '-', '') =
            replace(replace(trim(?), ' ', ''), '-', '')
      order by id desc
      limit 1
    """;

    List<String> out = jdbcTemplate.query(
      sql,
      ps -> ps.setString(1, vehicleNo),
      (rs, rowNum) -> rs.getString(1)
    );

    return out.isEmpty() ? null : out.get(0);
  }

  private DispatchResponse toRes(VehicleDispatch d) {
    return DispatchResponse.builder()
      .id(d.getId())
      .dispatchDate(d.getDispatchDate())
      .dispatchType(d.getDispatchType())
      .contractNumber(d.getContractNumber())
      .vehicleMgmtNo(d.getVehicleMgmtNo())
      .vehicleNo(d.getVehicleNo())
      .departureAddress(d.getDepartureAddress())
      .departureContact(d.getDepartureContact())
      .arrivalAddress(d.getArrivalAddress())
      .arrivalContact(d.getArrivalContact())
      .remarks(d.getRemarks())
      .createdAt(d.getCreatedAt())
      .updatedAt(d.getUpdatedAt())
      .build();
  }

  private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
  private String emptyToNull(String s) { return isBlank(s) ? null : s.trim(); }

  private String firstNonBlank(String a, String b) {
    if (!isBlank(a)) return a.trim();
    if (!isBlank(b)) return b.trim();
    return null;
  }
}