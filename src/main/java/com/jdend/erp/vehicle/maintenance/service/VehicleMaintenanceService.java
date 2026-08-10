package com.jdend.erp.vehicle.maintenance.service;

import com.jdend.erp.accounting.settings.service.OtherAccountSettingsService;
import com.jdend.erp.accounting.voucher.dto.VoucherCreateRequest;
import com.jdend.erp.accounting.voucher.dto.VoucherCreateResponse;
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
      String creditAccount = resolveCreditAccount(item.getPaymentMethod());
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

      // BUG-01 수정: 반환된 전표 ID를 정비 항목에 저장 → 정비 삭제 시 전표도 함께 삭제 가능
      VoucherCreateResponse vResponse = voucherService.create(
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
                      // BUG-02: 카드명/계좌명 상세는 계정명이 아닌 description에 기재
                      .description(buildCreditDescription(item.getPaymentMethod(), item.getPaymentDetail()))
                      .build()
              ))
              .build()
      );
      item.setVoucherId(vResponse.getId());
      itemRepository.save(item);
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

  /**
   * BUG-01 수정: 정비 기록 삭제 시 연관 전표도 함께 삭제하여 고아 전표 누적 방지.
   * 각 정비 항목에 저장된 voucherId를 수집 후 전표 삭제 → 정비 레코드 삭제 순서로 처리.
   */
  @Transactional
  public void delete(Long id) {
    VehicleMaintenance m = maintenanceRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("정비 기록이 없습니다: " + id));

    List<Long> voucherIds = m.getItems().stream()
        .map(VehicleMaintenanceItem::getVoucherId)
        .filter(vid -> vid != null)
        .toList();

    if (!voucherIds.isEmpty()) {
      voucherService.deleteByIds(voucherIds);
      log.info("정비 삭제로 연관 전표 자동 삭제: maintenanceId={}, voucherIds={}", id, voucherIds);
    }

    maintenanceRepository.delete(m);
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
   * BUG-02 수정: 결제수단별 대변 계정명 반환 — suffix(카드명/계좌명) 를 계정명에 붙이지 않는다.
   * suffix 를 붙이면 재무제표 원장 조회(exact match) 에서 "미지급비용" 을 못 찾는 문제가 있었음.
   * 카드명/계좌명 상세는 전표라인 description 에 기재한다.
   * 설정 미지정 시 null 반환 → 호출부에서 전표 건너뜀 처리.
   */
  private String resolveCreditAccount(String paymentMethod) {
    return switch (paymentMethod) {
      case "미지급금" -> accountSettings.getMaintenanceCreditUnpaidAccount();
      case "법인카드" -> accountSettings.getMaintenanceCreditCardAccount();
      case "보통예금" -> accountSettings.getMaintenanceCreditBankAccount();
      default -> throw new IllegalArgumentException("대변 계정 매핑 불가: " + paymentMethod);
    };
  }

  private String buildVoucherMemo(String vehicleNo, String description) {
    String vno = (vehicleNo == null || vehicleNo.isBlank()) ? "차량미상" : vehicleNo.trim();
    String desc = (description == null || description.isBlank()) ? "정비등록" : description.trim();
    return vno + " 정비등록 - " + desc;
  }

  /** BUG-02: 대변 전표라인 description — 결제수단(카드명/계좌명 포함) */
  private String buildCreditDescription(String paymentMethod, String paymentDetail) {
    if (paymentDetail != null && !paymentDetail.isBlank()) {
      return paymentMethod + "(" + paymentDetail.trim() + ")";
    }
    return paymentMethod == null ? "" : paymentMethod;
  }
}
