package com.jdend.erp.vehicle.sale.service;

import com.jdend.erp.accounting.depreciation.entity.DepreciationAsset;
import com.jdend.erp.accounting.depreciation.entity.DepreciationScheduleLine;
import com.jdend.erp.accounting.depreciation.repository.DepreciationAssetRepository;
import com.jdend.erp.accounting.depreciation.repository.DepreciationScheduleLineRepository;
import com.jdend.erp.accounting.settings.service.OtherAccountSettingsService;
import com.jdend.erp.accounting.voucher.dto.VoucherCreateRequest;
import com.jdend.erp.accounting.voucher.dto.VoucherCreateResponse;
import com.jdend.erp.accounting.voucher.repository.VoucherRepository;
import com.jdend.erp.accounting.voucher.service.VoucherService;
import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import com.jdend.erp.vehicle.sale.dto.VehicleSaleDtos;
import com.jdend.erp.vehicle.sale.entity.VehicleSale;
import com.jdend.erp.vehicle.sale.repository.VehicleSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleSaleService {

  private final VehicleSaleRepository saleRepo;
  private final VehicleOrderRepository vehicleOrderRepo;
  private final DepreciationAssetRepository depAssetRepo;
  private final DepreciationScheduleLineRepository depLineRepo;
  private final VoucherService voucherService;
  private final VoucherRepository voucherRepository;   // BUG-04: 전표 삭제용
  private final OtherAccountSettingsService accountSettings;

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

    // BUG-04: 전표 생성 후 ID를 VehicleSale에 저장
    Long voucherId = createSaleVoucher(saved);
    if (voucherId != null) {
      saved.setVoucherId(voucherId);
      saleRepo.save(saved);
    }

    return toRes(saved);
  }

  /**
   * BUG-04: 전표 생성 후 Voucher ID 반환
   * BUG-13: findByVehicleNo → findFirstByVehicleNoOrderByIdDesc (NonUniqueResultException 방지)
   */
  private Long createSaleVoucher(VehicleSale s) {
    long acquisitionCost = 0L;
    long accumulated = 0L;
    long remaining = 0L;

    // BUG-13: 동일 차량번호 자산이 여러 건일 때 최신 기준 1건만 사용
    DepreciationAsset asset = depAssetRepo.findFirstByVehicleNoOrderByIdDesc(s.getVehicleNo()).orElse(null);
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
      // 감가상각 미등록 시 차량 발주 총금액을 취득원가로 사용
      VehicleOrder vo = vehicleOrderRepo.findById(s.getVehicleOrderId()).orElse(null);
      if (vo != null && vo.getTotalPrice() != null && vo.getTotalPrice() > 0) {
        acquisitionCost = vo.getTotalPrice();
        remaining = acquisitionCost;
        accumulated = 0L;
      }
    }

    String saleDebitAccount    = accountSettings.getSaleDebitAccount();
    String saleRevenueAccount  = accountSettings.getSaleCreditAccount();
    if (saleDebitAccount == null || saleRevenueAccount == null) {
      log.warn("차량매각 전표 생략: 기타계정관리 > 차량매각 전표의 차변/대변을 설정해주세요. saleId={}", s.getId());
      return null;
    }

    String memo = (s.getVehicleNo() != null ? s.getVehicleNo() : s.getVehicleMgmtNo()) + " 차량 매각";

    List<VoucherCreateRequest.VoucherLineRequest> debits = new ArrayList<>();
    debits.add(line(saleDebitAccount, s.getSaleAmount(), memo));
    String accumDeprecAccount   = accountSettings.getSaleAccumDeprecAccount();
    String undepreciatedAccount = accountSettings.getSaleUndepreciatedAccount();
    if (accumulated > 0 && accumDeprecAccount != null)   debits.add(line(accumDeprecAccount,   accumulated, memo));
    if (remaining > 0   && undepreciatedAccount != null) debits.add(line(undepreciatedAccount, remaining,   memo));

    String vatCreditAccount    = accountSettings.getSaleVatCreditAccount();
    String vehicleAssetAccount = accountSettings.getSaleVehicleAssetAccount();

    List<VoucherCreateRequest.VoucherLineRequest> credits = new ArrayList<>();
    long revenueAmount = (vatCreditAccount != null) ? s.getSupplyAmount() : s.getSaleAmount();
    credits.add(line(saleRevenueAccount, revenueAmount, memo));
    if (vatCreditAccount != null)                           credits.add(line(vatCreditAccount,    s.getTaxAmount(), memo));
    if (acquisitionCost > 0 && vehicleAssetAccount != null) credits.add(line(vehicleAssetAccount, acquisitionCost,  memo));

    VoucherCreateResponse resp = voucherService.create(VoucherCreateRequest.builder()
        .voucherDate(s.getSaleDate())
        .vehicleNo(s.getVehicleNo())
        .vehicleMgmtNo(s.getVehicleMgmtNo())
        .contractNumber(null)
        .memo(memo)
        .debitEntries(debits)
        .creditEntries(credits)
        .build());
    return resp != null ? resp.getId() : null;
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

    // BUG-04: 기존 연결 전표 삭제
    if (s.getVoucherId() != null) {
      voucherRepository.findById(s.getVoucherId()).ifPresent(v -> {
        voucherRepository.delete(v);
        log.info("매각 수정: 기존 전표 삭제 voucherId={}", s.getVoucherId());
      });
      s.setVoucherId(null);
    }

    Calc calc = calcTax(req.saleAmount);

    s.setSaleDate(req.saleDate);
    s.setBuyer(req.buyer.trim());
    s.setSaleAmount(req.saleAmount);
    s.setSupplyAmount(calc.supply);
    s.setTaxAmount(calc.tax);

    if (!isBlank(req.status)) s.setStatus(req.status.trim());
    else if (isBlank(s.getStatus())) s.setStatus("완료");

    saleRepo.save(s);

    // BUG-04: 새 금액/날짜로 전표 재생성
    Long newVoucherId = createSaleVoucher(s);
    if (newVoucherId != null) {
      s.setVoucherId(newVoucherId);
      saleRepo.save(s);
    }

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

  // 매각금액 -> 공급가/세액 (부가세 10%, 포함가 역산)
  private Calc calcTax(long saleAmount) {
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
