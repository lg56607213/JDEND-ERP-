package com.jdend.erp.vehicle.sale.service;

import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import com.jdend.erp.vehicle.sale.dto.VehicleSaleDtos;
import com.jdend.erp.vehicle.sale.entity.VehicleSale;
import com.jdend.erp.vehicle.sale.repository.VehicleSaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleSaleService {

  private final VehicleSaleRepository saleRepo;
  private final VehicleOrderRepository vehicleOrderRepo;

  @Transactional
  public VehicleSaleDtos.Response create(VehicleSaleDtos.CreateRequest req) {
    if (req == null) throw new RuntimeException("요청값이 비었습니다.");
    if (isBlank(req.vehicleMgmtNo)) throw new RuntimeException("vehicleMgmtNo 필수");
    if (req.saleDate == null) throw new RuntimeException("saleDate 필수");
    if (isBlank(req.buyer)) throw new RuntimeException("buyer 필수");
    if (req.saleAmount == null || req.saleAmount <= 0) throw new RuntimeException("saleAmount 필수");

    VehicleOrder vo = vehicleOrderRepo.findByVehicleMgmtNo(req.vehicleMgmtNo.trim())
      .orElseThrow(() -> new RuntimeException("차량 없음: " + req.vehicleMgmtNo));

    Calc calc = calcTax(req.saleAmount);

    VehicleSale s = VehicleSale.builder()
      .vehicleOrderId(vo.getId())
      .vehicleMgmtNo(vo.getVehicleMgmtNo())
      .vehicleNo(firstNonBlank(req.vehicleNo, vo.getVehicleNo()))
      .carModel(vo.getCarModel())
      .chassisNo(vo.getChassisNo())
      .saleDate(req.saleDate)
      .buyer(req.buyer.trim())
      .saleAmount(req.saleAmount)
      .supplyAmount(calc.supply)
      .taxAmount(calc.tax)
      .status("완료")
      .build();

    VehicleSale saved = saleRepo.save(s);
    return toRes(saved);
  }

  @Transactional(readOnly = true)
  public List<VehicleSaleDtos.Response> list(LocalDate startDate, LocalDate endDate, String buyer) {
    return saleRepo.search(startDate, endDate, emptyToNull(buyer))
      .stream().map(this::toRes).toList();
  }

  @Transactional(readOnly = true)
  public VehicleSaleDtos.Response detail(Long id) {
    VehicleSale s = saleRepo.findById(id).orElseThrow(() -> new RuntimeException("매각 없음: " + id));
    return toRes(s);
  }

  @Transactional
  public VehicleSaleDtos.Response update(Long id, VehicleSaleDtos.UpdateRequest req) {
    if (req == null) throw new RuntimeException("요청값이 비었습니다.");
    if (req.saleDate == null) throw new RuntimeException("saleDate 필수");
    if (isBlank(req.buyer)) throw new RuntimeException("buyer 필수");
    if (req.saleAmount == null || req.saleAmount <= 0) throw new RuntimeException("saleAmount 필수");

    VehicleSale s = saleRepo.findById(id).orElseThrow(() -> new RuntimeException("매각 없음: " + id));

    Calc calc = calcTax(req.saleAmount);

    s.setSaleDate(req.saleDate);
    s.setBuyer(req.buyer.trim());
    s.setSaleAmount(req.saleAmount);
    s.setSupplyAmount(calc.supply);
    s.setTaxAmount(calc.tax);

    if (!isBlank(req.status)) s.setStatus(req.status.trim());
    else if (isBlank(s.getStatus())) s.setStatus("완료");

    saleRepo.save(s);
    return toRes(s);
  }

  private VehicleSaleDtos.Response toRes(VehicleSale s) {
    return VehicleSaleDtos.Response.builder()
      .id(s.getId())
      .vehicleOrderId(s.getVehicleOrderId())
      .vehicleMgmtNo(s.getVehicleMgmtNo())
      .vehicleNo(s.getVehicleNo())
      .carModel(s.getCarModel())
      .chassisNo(s.getChassisNo())
      .saleDate(s.getSaleDate())
      .buyer(s.getBuyer())
      .saleAmount(s.getSaleAmount())
      .supplyAmount(s.getSupplyAmount())
      .taxAmount(s.getTaxAmount())
      .status(s.getStatus())
      .createdAt(s.getCreatedAt())
      .updatedAt(s.getUpdatedAt())
      .build();
  }

  // 매각금액 -> 공급가/세액 (부가세 10%)
  private Calc calcTax(long saleAmount) {
    // 공급가액 = round(매각금액 / 1.1)
    long supply = Math.round(saleAmount / 1.1d);
    long tax = saleAmount - supply;
    return new Calc(supply, tax);
  }

  private record Calc(long supply, long tax) {}

  private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
  private String emptyToNull(String s) { return isBlank(s) ? null : s.trim(); }

  private String firstNonBlank(String a, String b) {
    if (!isBlank(a)) return a.trim();
    if (!isBlank(b)) return b.trim();
    return null;
  }
}