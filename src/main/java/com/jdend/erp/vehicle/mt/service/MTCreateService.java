package com.jdend.erp.vehicle.mt.service;

import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.mt.dto.MTCreateRequest;
import com.jdend.erp.vehicle.mt.dto.MTCreateResponse;
import com.jdend.erp.vehicle.maintenance.entity.VehicleMaintenance;
import com.jdend.erp.vehicle.maintenance.entity.VehicleMaintenanceItem;
import com.jdend.erp.vehicle.maintenance.repository.VehicleMaintenanceRepository;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MTCreateService {

  private final VehicleOrderRepository vehicleOrderRepository;
  private final VehicleMaintenanceRepository maintenanceRepository;

  @Transactional
  public MTCreateResponse create(MTCreateRequest req) {
    if (req == null) throw new IllegalArgumentException("요청 바디가 비었습니다.");

    String mgmtNo = req.getVehicleMgmtNo() == null ? "" : req.getVehicleMgmtNo().trim();
    if (mgmtNo.isEmpty()) throw new IllegalArgumentException("vehicleMgmtNo는 필수입니다.");

    String vendor = req.getVendor() == null ? "" : req.getVendor().trim();
    if (vendor.isEmpty()) throw new IllegalArgumentException("vendor(MT업체)는 필수입니다.");

    String type = req.getMaintenanceType() == null ? "" : req.getMaintenanceType().trim();
    if (type.isEmpty()) throw new IllegalArgumentException("maintenanceType(정비종류)는 필수입니다.");

    if (req.getRetrieveDate() == null) throw new IllegalArgumentException("retrieveDate(회수의뢰일)는 필수입니다.");

    long cost = req.getMtCost() == null ? 0L : req.getMtCost();
    if (cost <= 0) throw new IllegalArgumentException("mtCost(MT비용)는 1 이상이어야 합니다.");

    VehicleOrder v = vehicleOrderRepository.findByVehicleMgmtNo(mgmtNo)
        .orElseThrow(() -> new IllegalArgumentException("차량관리번호를 찾을 수 없습니다: " + mgmtNo));

    VehicleMaintenance m = new VehicleMaintenance();
    m.setVehicleOrderId(v.getId());
    m.setVehicleMgmtNo(v.getVehicleMgmtNo());
    m.setVehicleNo((req.getVehicleNo() != null && !req.getVehicleNo().isBlank())
        ? req.getVehicleNo().trim()
        : v.getVehicleNo());

    VehicleMaintenanceItem item = new VehicleMaintenanceItem();
    item.setDescription(type);            // 정비종류
    item.setVendor(vendor);
    item.setAmount(cost);

    long supply = Math.round(cost / 1.1);
    long vat = cost - supply;
    item.setSupplyAmount(supply);
    item.setVatAmount(vat);

    item.setPayDate(req.getRetrieveDate()); // 회수의뢰일 = payDate

    // 연관관계 세팅 (VehicleMaintenance 엔티티에 addItem이 있어야 함)
    m.addItem(item);

    VehicleMaintenance saved = maintenanceRepository.save(m);

    // 첫 item id 반환 (등록은 1건만)
    Long itemId = saved.getItems().get(0).getId();
    return new MTCreateResponse(saved.getId(), itemId);
  }
}