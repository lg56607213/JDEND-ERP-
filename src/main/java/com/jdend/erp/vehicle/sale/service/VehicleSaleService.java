package com.jdend.erp.vehicle.sale.service;

import com.jdend.erp.accounting.depreciation.entity.DepreciationAsset;
import com.jdend.erp.accounting.depreciation.entity.DepreciationScheduleLine;
import com.jdend.erp.accounting.depreciation.repository.DepreciationAssetRepository;
import com.jdend.erp.accounting.depreciation.repository.DepreciationScheduleLineRepository;
import com.jdend.erp.accounting.voucher.dto.VoucherCreateRequest;
import com.jdend.erp.accounting.voucher.service.VoucherService;
import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import com.jdend.erp.vehicle.sale.dto.VehicleSaleDtos;
import com.jdend.erp.vehicle.sale.entity.VehicleSale;
import com.jdend.erp.vehicle.sale.repository.VehicleSaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleSaleService {

  private final VehicleSaleRepository saleRepo;
  private final VehicleOrderRepository vehicleOrderRepo;
  private final DepreciationAssetRepository depAssetRepo;
  private final DepreciationScheduleLineRepository depLineRepo;
  private final VoucherService voucherService;

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
    createSaleVoucher(saved);
    return toRes(saved);
  }

  private void createSaleVoucher(VehicleSale s) {
    long acquisitionCost = 0L;
    long accumulated = 0L;
    long remaining = 0L;

    DepreciationAsset asset = depAssetRepo.findByVehicleNo(s.getVehicleNo()).orElse(null);
    if (asset != null) {
      acquisitionCost = asset.getAcquisitionCost();
      int ver = depLineRepo.findMaxVersion(asset.getId());
      if (ver == 0) ver = 1;
      DepreciationScheduleLine latest = depLineRepo
          .findLatestLineUpTo(asset.getId(), ver, s.getSaleDate())
          .stream().findFirst().orElse(null);
      remaining = (latest == null) ? acquisitionCost : latest.getBalance();
      accumulated = acquisitionCost - remaining;
    } else {
      // 감가상각 미등록 시 차량 발주 총금액을 취득원가로 사용, 전액 미상각 처리
      VehicleOrder vo = vehicleOrderRepo.findById(s.getVehicleOrderId()).orElse(null);
      if (vo != null && vo.getTotalPrice() != null && vo.getTotalPrice() > 0) {
        acquisitionCost = vo.getTotalPrice();
        remaining = acquisitionCost;
        accumulated = 0L;
      }
    }

    String memo = (s.getVehicleNo() != null ? s.getVehicleNo() : s.getVehicleMgmtNo()) + " 차량 매각";

    List<VoucherCreateRequest.VoucherLineRequest> debits = new ArrayList<>();
    debits.add(line("미수금", s.getSaleAmount(), memo));
    if (accumulated > 0) debits.add(line("감가상각누계액", accumulated, memo));
    if (remaining > 0)   debits.add(line("미상각잔액", remaining, memo));

    List<VoucherCreateRequest.VoucherLineRequest> credits = new ArrayList<>();
    credits.add(line("판매수익", s.getSupplyAmount(), memo));
    credits.add(line("부가세예수금", s.getTaxAmount(), memo));
    if (acquisitionCost > 0) credits.add(line("차량운반구", acquisitionCost, memo));

    voucherService.create(VoucherCreateRequest.builder()
        .voucherDate(s.getSaleDate())
        .vehicleNo(s.getVehicleNo())
        .contractNumber(null)
        .memo(memo)
        .debitEntries(debits)
        .creditEntries(credits)
        .build());
  }

  private VoucherCreateRequest.VoucherLineRequest line(String account, long amount, String desc) {
    return VoucherCreateRequest.VoucherLineRequest.builder()
        .account(account).amount(amount).description(desc).build();
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