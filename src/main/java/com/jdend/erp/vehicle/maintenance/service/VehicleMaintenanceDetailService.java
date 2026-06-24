package com.jdend.erp.vehicle.maintenance.service;

import com.jdend.erp.vehicle.maintenance.dto.VehicleMaintenanceItemDetailResponse;
import com.jdend.erp.vehicle.maintenance.dto.VehicleMaintenanceItemUpdateRequest;
import com.jdend.erp.vehicle.maintenance.entity.VehicleMaintenance;
import com.jdend.erp.vehicle.maintenance.entity.VehicleMaintenanceItem;
import com.jdend.erp.vehicle.maintenance.repository.VehicleMaintenanceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class VehicleMaintenanceDetailService {

  private final VehicleMaintenanceItemRepository itemRepository;

  @Transactional(readOnly = true)
  public VehicleMaintenanceItemDetailResponse getDetail(Long maintenanceId, Long itemId) {
    if (maintenanceId == null || itemId == null) {
      throw new IllegalArgumentException("maintenanceId, itemId는 필수입니다.");
    }

    VehicleMaintenanceItem item = itemRepository.findByIdAndMaintenance_Id(itemId, maintenanceId)
        .orElseThrow(() -> new IllegalArgumentException(
            "정비 상세를 찾을 수 없습니다. maintenanceId=" + maintenanceId + ", itemId=" + itemId
        ));

    VehicleMaintenance m = item.getMaintenance();
    LocalDate maintenanceDate = item.getPayDate();

    return VehicleMaintenanceItemDetailResponse.builder()
        .maintenanceId(m.getId())
        .itemId(item.getId())
        .vehicleMgmtNo(m.getVehicleMgmtNo())
        .vehicleNo(m.getVehicleNo())
        .maintenanceDate(maintenanceDate)
        .payDate(item.getPayDate())
        .description(item.getDescription())
        .amount(nvl(item.getAmount()))
        .supplyAmount(nvl(item.getSupplyAmount()))
        .vatAmount(nvl(item.getVatAmount()))
        .vendor(item.getVendor())
        .paymentMethod(item.getPaymentMethod())
        .build();
  }

  @Transactional
  public VehicleMaintenanceItemDetailResponse update(Long itemId, VehicleMaintenanceItemUpdateRequest req) {
    if (itemId == null) throw new IllegalArgumentException("itemId는 필수입니다.");
    if (req == null) throw new IllegalArgumentException("요청 바디가 비었습니다.");

    VehicleMaintenanceItem item = itemRepository.findById(itemId)
        .orElseThrow(() -> new IllegalArgumentException("정비 항목을 찾을 수 없습니다. itemId=" + itemId));

    String desc = req.getDescription() == null ? "" : req.getDescription().trim();
    if (desc.isEmpty()) throw new IllegalArgumentException("정비내용(description)은 필수입니다.");

    long amount = nvl(req.getAmount());
    long supply = nvl(req.getSupplyAmount());
    long vat = nvl(req.getVatAmount());

    item.setDescription(desc);
    item.setAmount(amount);
    item.setSupplyAmount(supply);
    item.setVatAmount(vat);
    item.setVendor(trimOrNull(req.getVendor()));
    item.setPaymentMethod(normalizePaymentMethod(req.getPaymentMethod()));

    LocalDate payDate = (req.getPayDate() != null) ? req.getPayDate() : req.getMaintenanceDate();
    item.setPayDate(payDate);

    VehicleMaintenanceItem saved = itemRepository.save(item);
    VehicleMaintenance m = saved.getMaintenance();

    return VehicleMaintenanceItemDetailResponse.builder()
        .maintenanceId(m.getId())
        .itemId(saved.getId())
        .vehicleMgmtNo(m.getVehicleMgmtNo())
        .vehicleNo(m.getVehicleNo())
        .maintenanceDate(saved.getPayDate())
        .payDate(saved.getPayDate())
        .description(saved.getDescription())
        .amount(nvl(saved.getAmount()))
        .supplyAmount(nvl(saved.getSupplyAmount()))
        .vatAmount(nvl(saved.getVatAmount()))
        .vendor(saved.getVendor())
        .paymentMethod(saved.getPaymentMethod())
        .build();
  }

  private long nvl(Long v) {
    return v == null ? 0L : v;
  }

  private String trimOrNull(String s) {
    if (s == null) return null;
    String t = s.trim();
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
}