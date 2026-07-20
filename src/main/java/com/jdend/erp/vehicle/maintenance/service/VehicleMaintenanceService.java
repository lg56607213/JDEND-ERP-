package com.jdend.erp.vehicle.maintenance.service;

import com.jdend.erp.accounting.settings.service.OtherAccountSettingsService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleMaintenanceService {

  private final VehicleOrderRepository vehicleOrderRepository;
  private final VehicleMaintenanceRepository maintenanceRepository;
  private final VehicleMaintenanceItemRepository itemRepository;
  private final VoucherService voucherService;
  private final OtherAccountSettingsService accountSettings;

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
      item.setPaymentDetail(trimOrNull(it.getPaymentDetail()));

      m.addItem(item);
    }

    VehicleMaintenance saved = maintenanceRepository.save(m);

    for (VehicleMaintenanceItem item : saved.getItems()) {
      long supply = nvlLong(item.getSupplyAmount());
      long vat    = nvlLong(item.getVatAmount());
      long total  = nvlLong(item.getAmount());

      // 차변 계정 설정 확인 — 미설정이면 전표 전체 건너뜀
      String debitAccount = accountSettings.getMaintenanceDebitAccount();
      if (debitAccount == null) {
        log.warn("정비 차변 계정(maintenanceMapping.debit) 미설정으로 전표를 건너뜁니다. description={}", item.getDescription());
        continue;
      }

      // 대변 계정 설정 확인 — 미설정이면 전표 전체 건너뜀
      String creditAccount = resolveCreditAccountWithDetail(item.getPaymentMethod(), item.getPaymentDetail());
      if (creditAccount == null) {
        log.warn("정비 대변 계정 미설정으로 전표를 건너뜁니다. paymentMethod={}, description={}",
            item.getPaymentMethod(), item.getDescription());
        continue;
      }

      // 부가세 차변 계정 — 미설정이면 부가세 분개 라인만 생략
      String vatDebitAccount = accountSettings.getMaintenanceVatDebitAccount();

      List<VoucherCreateRequest.VoucherLineRequest> debits = new ArrayList<>();
      if (supply > 0) {
        debits.add(VoucherCreateRequest.VoucherLineRequest.builder()
            .account(debitAccount)
            .amount(supply)
            .description(item.getDescription())
            .build());
      }
      if (vat > 0) {
        if (vatDebitAccount != null) {
          debits.add(VoucherCreateRequest.VoucherLineRequest.builder()
              .account(vatDebitAccount)
              .amount(vat)
              .description(item.getDescription())
              .build());
        } else {
          log.warn("정비 부가세 차변 계정(maintenanceMapping.vatDebit) 미설정으로 부가세 분개를 건너뜁니다. description={}", item.getDescription());
        }
      }
      if (debits.isEmpty()) {
        debits.add(VoucherCreateRequest.VoucherLineRequest.builder()
            .account(debitAccount)
            .amount(total)
            .description(item.getDescription())
            .build());
      }

      long creditTotal = debits.stream().mapToLong(VoucherCreateRequest.VoucherLineRequest::getAmount).sum();

      voucherService.create(
          VoucherCreateRequest.builder()
              .voucherDate(item.getPayDate() != null ? item.getPayDate() : LocalDate.now())
              .vehicleNo(saved.getVehicleNo())
              .vehicleMgmtNo(saved.getVehicleMgmtNo())
              .memo(buildVoucherMemo(saved.getVehicleNo(), item.getDescription()))
              .debitEntries(debits)
              .creditEntries(List.of(
                  VoucherCreateRequest.VoucherLineRequest.builder()
                      .account(creditAccount)
                      .amount(creditTotal)
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

  /**
   * 결제수단별 대변 계정명 반환. suffix("(xxx)") 로직은 기존 그대로 유지.
   * - 미지급금: suffix 없음
   * - 법인카드/보통예금: paymentDetail suffix 추가
   * 설정 미지정 시 null 반환 → 호출부에서 전표 건너뜀 처리.
   */
  private String resolveCreditAccountWithDetail(String paymentMethod, String paymentDetail) {
    String suffix = (paymentDetail != null && !paymentDetail.isBlank()) ? "(" + paymentDetail.trim() + ")" : "";
    return switch (paymentMethod) {
      case "미지급금" -> accountSettings.getMaintenanceCreditUnpaidAccount();
      case "법인카드" -> {
        String base = accountSettings.getMaintenanceCreditCardAccount();
        yield base == null ? null : base + suffix;
      }
      case "보통예금" -> {
        String base = accountSettings.getMaintenanceCreditBankAccount();
        yield base == null ? null : base + suffix;
      }
      default -> throw new IllegalArgumentException("대변 계정 매핑 불가: " + paymentMethod);
    };
  }

  private String buildVoucherMemo(String vehicleNo, String description) {
    String vno = (vehicleNo == null || vehicleNo.isBlank()) ? "차량미상" : vehicleNo.trim();
    String desc = (description == null || description.isBlank()) ? "정비등록" : description.trim();
    return vno + " 정비등록 - " + desc;
  }
}
