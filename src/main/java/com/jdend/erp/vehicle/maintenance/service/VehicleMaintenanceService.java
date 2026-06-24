package com.jdend.erp.vehicle.maintenance.service;

import com.jdend.erp.accounting.voucher.dto.VoucherCreateRequest;
import com.jdend.erp.accounting.voucher.service.VoucherService;
import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.maintenance.dto.VehicleMaintenanceCreateRequest;
import com.jdend.erp.vehicle.maintenance.dto.VehicleMaintenanceItemRequest;
import com.jdend.erp.vehicle.maintenance.dto.VehicleMaintenanceResponse;
import com.jdend.erp.vehicle.maintenance.dto.VehicleMaintenanceStatusRowResponse;
import com.jdend.erp.vehicle.maintenance.dto.VehicleMaintenanceVehicleInfoResponse;
import com.jdend.erp.vehicle.maintenance.entity.VehicleMaintenance;
import com.jdend.erp.vehicle.maintenance.entity.VehicleMaintenanceItem;
import com.jdend.erp.vehicle.maintenance.repository.VehicleMaintenanceItemRepository;
import com.jdend.erp.vehicle.maintenance.repository.VehicleMaintenanceRepository;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleMaintenanceService {

  private final VehicleOrderRepository vehicleOrderRepository;
  private final VehicleMaintenanceRepository maintenanceRepository;
  private final VehicleMaintenanceItemRepository itemRepository;
  private final VoucherService voucherService;

  @Transactional(readOnly = true)
  public VehicleMaintenanceVehicleInfoResponse getVehicleInfoByMgmtNo(String mgmtNo) {
    String key = (mgmtNo == null) ? "" : mgmtNo.trim();
    if (key.isEmpty()) throw new IllegalArgumentException("차량관리번호(mgmtNo)는 필수입니다.");

    VehicleOrder v = vehicleOrderRepository.findByVehicleMgmtNo(key)
        .orElseThrow(() -> new IllegalArgumentException("차량관리번호를 찾을 수 없습니다: " + key));

    return new VehicleMaintenanceVehicleInfoResponse(
        v.getVehicleMgmtNo(),
        v.getVehicleNo(),
        v.getInspectionStart(),
        v.getInspectionEnd()
    );
  }

  @Transactional
  public VehicleMaintenanceResponse create(VehicleMaintenanceCreateRequest req) {
    if (req == null) throw new IllegalArgumentException("요청 바디가 비었습니다.");

    String mgmtNo = (req.getVehicleMgmtNo() == null) ? "" : req.getVehicleMgmtNo().trim();
    if (mgmtNo.isEmpty()) throw new IllegalArgumentException("vehicleMgmtNo는 필수입니다.");
    if (req.getItems() == null || req.getItems().isEmpty()) {
      throw new IllegalArgumentException("정비 내역(items)은 1개 이상 필요합니다.");
    }

    VehicleOrder v = vehicleOrderRepository.findByVehicleMgmtNo(mgmtNo)
        .orElseThrow(() -> new IllegalArgumentException("차량관리번호를 찾을 수 없습니다: " + mgmtNo));

    VehicleMaintenance m = new VehicleMaintenance();
    m.setVehicleOrderId(v.getId());
    m.setVehicleMgmtNo(v.getVehicleMgmtNo());
    m.setVehicleNo(v.getVehicleNo());

    for (VehicleMaintenanceItemRequest it : req.getItems()) {
      String desc = it.getDescription() == null ? "" : it.getDescription().trim();
      if (desc.isEmpty()) throw new IllegalArgumentException("정비 내역의 '내용(description)'은 필수입니다.");

      String paymentMethod = normalizePaymentMethod(it.getPaymentMethod());

      VehicleMaintenanceItem item = new VehicleMaintenanceItem();
      item.setDescription(desc);
      item.setAmount(nvlLong(it.getAmount()));
      item.setSupplyAmount(nvlLong(it.getSupplyAmount()));
      item.setVatAmount(nvlLong(it.getVatAmount()));
      item.setVendor(trimOrNull(it.getVendor()));
      item.setPayDate(it.getPayDate());
      item.setPaymentMethod(paymentMethod);

      m.addItem(item);
    }

    VehicleMaintenance saved = maintenanceRepository.save(m);

    for (VehicleMaintenanceItem item : saved.getItems()) {
      voucherService.create(
          VoucherCreateRequest.builder()
              .voucherDate(item.getPayDate() != null ? item.getPayDate() : LocalDate.now())
              .vehicleNo(saved.getVehicleNo())
              .memo(buildVoucherMemo(saved.getVehicleNo(), item.getDescription()))
              .debitEntries(List.of(
                  VoucherCreateRequest.VoucherLineRequest.builder()
                      .account("차량유지비")
                      .amount(nvlLong(item.getAmount()))
                      .description(item.getDescription())
                      .build()
              ))
              .creditEntries(List.of(
                  VoucherCreateRequest.VoucherLineRequest.builder()
                      .account(resolveCreditAccount(item.getPaymentMethod()))
                      .amount(nvlLong(item.getAmount()))
                      .description(item.getPaymentMethod())
                      .build()
              ))
              .build()
      );
    }

    return new VehicleMaintenanceResponse(saved.getId());
  }

  @Transactional(readOnly = true)
  public List<VehicleMaintenanceStatusRowResponse> searchStatus(
      String vehicleMgmtNo,
      String vehicleNo,
      LocalDate startDate,
      LocalDate endDate
  ) {
    String mgmt = vehicleMgmtNo == null ? "" : vehicleMgmtNo.trim();
    String vno = vehicleNo == null ? "" : vehicleNo.trim();
    return itemRepository.searchStatus(mgmt, vno, startDate, endDate);
  }

  private Long nvlLong(Long v) {
    return v == null ? 0L : v;
  }

  private String trimOrNull(String v) {
    if (v == null) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }

  private String normalizePaymentMethod(String s) {
    String t = (s == null) ? "" : s.trim();
    if (t.isEmpty()) return "미지급금";

    return switch (t) {
      case "미지급금", "법인카드", "보통예금" -> t;
      default -> throw new IllegalArgumentException("지원하지 않는 지급방법입니다: " + t);
    };
  }

  private String resolveCreditAccount(String paymentMethod) {
    return switch (paymentMethod) {
      case "미지급금" -> "미지급금(렌트)";
      case "법인카드" -> "미지급비용(법인카드)";
      case "보통예금" -> "보통예금";
      default -> throw new IllegalArgumentException("대변 계정 매핑 불가: " + paymentMethod);
    };
  }

  private String buildVoucherMemo(String vehicleNo, String description) {
    String vno = (vehicleNo == null || vehicleNo.isBlank()) ? "차량미상" : vehicleNo.trim();
    String desc = (description == null || description.isBlank()) ? "정비등록" : description.trim();
    return vno + " 정비등록 - " + desc;
  }
}