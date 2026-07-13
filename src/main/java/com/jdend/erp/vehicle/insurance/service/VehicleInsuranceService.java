package com.jdend.erp.vehicle.insurance.service;

import com.jdend.erp.accounting.voucher.entity.Voucher;
import com.jdend.erp.accounting.voucher.entity.VoucherLine;
import com.jdend.erp.accounting.voucher.repository.VoucherRepository;
import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.insurance.dto.VehicleInsuranceDtos;
import com.jdend.erp.vehicle.insurance.entity.InsuranceChange;
import com.jdend.erp.vehicle.insurance.entity.VehicleInsurance;
import com.jdend.erp.vehicle.insurance.repository.InsuranceChangeRepository;
import com.jdend.erp.vehicle.insurance.repository.VehicleInsuranceRepository;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleInsuranceService {

  private final VehicleInsuranceRepository insuranceRepo;
  private final VehicleOrderRepository vehicleOrderRepo;
  private final VoucherRepository voucherRepository;
  private final InsuranceChangeRepository insuranceChangeRepo;
  private final JdbcTemplate jdbcTemplate;

  @Transactional
  public VehicleInsuranceDtos.Response create(VehicleInsuranceDtos.CreateRequest req) {
    if (req == null) throw new RuntimeException("요청값이 비었습니다.");
    if (isBlank(req.vehicleMgmtNo)) throw new RuntimeException("vehicleMgmtNo 필수");
    if (req.insuranceStartDate == null) throw new RuntimeException("insuranceStartDate 필수");
    if (req.insuranceEndDate == null) throw new RuntimeException("insuranceEndDate 필수");
    if (req.insurancePremium == null || req.insurancePremium <= 0) throw new RuntimeException("insurancePremium 필수");

    VehicleOrder vo = vehicleOrderRepo.findByVehicleMgmtNo(req.vehicleMgmtNo.trim())
      .orElseThrow(() -> new RuntimeException("차량 없음: " + req.vehicleMgmtNo));

    String vehicleNo = firstNonBlank(req.vehicleNo, vo.getVehicleNo());
    String contractNumber = findLatestContractNumberByVehicleNo(vehicleNo);

    VehicleInsurance i = VehicleInsurance.builder()
      .vehicleOrderId(vo.getId())
      .vehicleMgmtNo(vo.getVehicleMgmtNo())
      .vehicleNo(emptyToNull(vehicleNo))
      .contractNumber(emptyToNull(contractNumber))

      .insuranceStartDate(req.insuranceStartDate)
      .insuranceEndDate(req.insuranceEndDate)

      .bodilyInjury(emptyToNull(req.bodilyInjury))
      .propertyDamage(emptyToNull(req.propertyDamage))
      .personalInjury(emptyToNull(req.personalInjury))
      .autoInjury(emptyToNull(req.autoInjury))
      .uninsured(emptyToNull(req.uninsured))
      .ownVehicle(emptyToNull(req.ownVehicle))
      .emergency(emptyToNull(req.emergency))

      .ageRange(emptyToNull(req.ageRange))
      .driverRange(emptyToNull(req.driverRange))

      .specialTerms(emptyToNull(req.specialTerms))
      .insurancePremium(req.insurancePremium)
      .build();

    VehicleInsurance saved = insuranceRepo.save(i);

    createInsuranceVoucher(saved, req.voucherDate);

    return toRes(saved);
  }

  @Transactional(readOnly = true)
  public List<VehicleInsuranceDtos.Response> list(String contractNumber, String vehicleNo, LocalDate startDate, LocalDate endDate) {
    return insuranceRepo.search(
      emptyToNull(contractNumber),
      emptyToNull(vehicleNo),
      startDate,
      endDate
    ).stream().map(this::toRes).toList();
  }

  @Transactional(readOnly = true)
  public VehicleInsuranceDtos.Response detail(Long id) {
    VehicleInsurance i = insuranceRepo.findById(id).orElseThrow(() -> new RuntimeException("보험 없음: " + id));
    return toRes(i);
  }

  @Transactional
  public VehicleInsuranceDtos.Response update(Long id, VehicleInsuranceDtos.UpdateRequest req) {
    if (req == null) throw new RuntimeException("요청값이 비었습니다.");
    if (req.insuranceStartDate == null) throw new RuntimeException("insuranceStartDate 필수");
    if (req.insuranceEndDate == null) throw new RuntimeException("insuranceEndDate 필수");
    if (req.insurancePremium == null || req.insurancePremium <= 0) throw new RuntimeException("insurancePremium 필수");

    VehicleInsurance i = insuranceRepo.findById(id).orElseThrow(() -> new RuntimeException("보험 없음: " + id));

    i.setInsuranceStartDate(req.insuranceStartDate);
    i.setInsuranceEndDate(req.insuranceEndDate);

    i.setBodilyInjury(emptyToNull(req.bodilyInjury));
    i.setPropertyDamage(emptyToNull(req.propertyDamage));
    i.setPersonalInjury(emptyToNull(req.personalInjury));
    i.setAutoInjury(emptyToNull(req.autoInjury));
    i.setUninsured(emptyToNull(req.uninsured));
    i.setOwnVehicle(emptyToNull(req.ownVehicle));
    i.setEmergency(emptyToNull(req.emergency));

    i.setAgeRange(emptyToNull(req.ageRange));
    i.setDriverRange(emptyToNull(req.driverRange));

    i.setSpecialTerms(emptyToNull(req.specialTerms));
    i.setInsurancePremium(req.insurancePremium);

    insuranceRepo.save(i);
    return toRes(i);
  }

  @Transactional
  public void change(Long insuranceId, VehicleInsuranceDtos.InsuranceChangeRequest req) {
    VehicleInsurance insurance = insuranceRepo.findById(insuranceId)
      .orElseThrow(() -> new RuntimeException("보험 없음: " + insuranceId));

    LocalDate vd = req.voucherDate != null ? req.voucherDate : LocalDate.now();
    String type = (req.changeType != null && !req.changeType.isBlank()) ? req.changeType : "변경";

    insuranceChangeRepo.save(InsuranceChange.builder()
      .insuranceId(insuranceId)
      .changeType(type)
      .changeReason(req.changeReason)
      .additionalPremium(req.additionalPremium)
      .refundPremium(req.refundPremium)
      .voucherDate(vd)
      .build());

    String baseMemo = "보험" + type + " / "
      + (insurance.getVehicleNo() != null ? "차량: " + insurance.getVehicleNo() + " / " : "")
      + (req.changeReason != null && !req.changeReason.isBlank() ? req.changeReason : "");

    if (req.additionalPremium != null && req.additionalPremium > 0) {
      createSingleVoucher(insurance, vd, req.additionalPremium,
        "보험료", "미지급금(보험료)", baseMemo + " (추가납부)");
    }
    if (req.refundPremium != null && req.refundPremium > 0) {
      createSingleVoucher(insurance, vd, req.refundPremium,
        "미수금(보험료)", "보험료", baseMemo + " (환급)");
    }
  }

  private void createSingleVoucher(VehicleInsurance insurance, LocalDate voucherDate,
                                    Long amount, String debitAccount, String creditAccount, String memo) {
    String voucherNo = nextVoucherNo(voucherDate);

    Voucher voucher = Voucher.builder()
      .voucherNo(voucherNo)
      .voucherDate(voucherDate)
      .contractNumber(emptyToNull(insurance.getContractNumber()))
      .vehicleNo(emptyToNull(insurance.getVehicleNo()))
      .vehicleMgmtNo(emptyToNull(insurance.getVehicleMgmtNo()))
      .totalAmount(amount)
      .status("대기")
      .memo(memo)
      .build();

    voucher.addLine(VoucherLine.builder()
      .lineType("DEBIT")
      .accountName(debitAccount)
      .amount(amount)
      .description(memo)
      .sortOrder(1)
      .build());

    voucher.addLine(VoucherLine.builder()
      .lineType("CREDIT")
      .accountName(creditAccount)
      .amount(amount)
      .description(memo)
      .sortOrder(2)
      .build());

    voucherRepository.save(voucher);
  }

  private void createInsuranceVoucher(VehicleInsurance insurance, LocalDate requestedVoucherDate) {
    Long amount = insurance.getInsurancePremium();
    if (amount == null || amount <= 0) return;

    LocalDate voucherDate = requestedVoucherDate != null ? requestedVoucherDate :
      (insurance.getInsuranceStartDate() != null ? insurance.getInsuranceStartDate() : LocalDate.now());

    String voucherNo = nextVoucherNo(voucherDate);

    Voucher voucher = Voucher.builder()
      .voucherNo(voucherNo)
      .voucherDate(voucherDate)
      .contractNumber(emptyToNull(insurance.getContractNumber()))
      .vehicleNo(emptyToNull(insurance.getVehicleNo()))
      .vehicleMgmtNo(emptyToNull(insurance.getVehicleMgmtNo()))
      .totalAmount(amount)
      .status("대기")
      .memo(buildInsuranceVoucherMemo(insurance))
      .build();

    voucher.addLine(VoucherLine.builder()
      .lineType("DEBIT")
      .accountName("보험료")
      .amount(amount)
      .description("보험등록 보험료")
      .sortOrder(1)
      .build());

    voucher.addLine(VoucherLine.builder()
      .lineType("CREDIT")
      .accountName("미지급금(보험료)")
      .amount(amount)
      .description("보험등록 보험료")
      .sortOrder(2)
      .build());

    voucherRepository.save(voucher);
  }

  private String nextVoucherNo(LocalDate date) {
    long cnt = voucherRepository.countByVoucherDate(date);
    long next = cnt + 1;
    String ymd = date.toString().replace("-", "");
    return "V" + ymd + "-" + String.format("%03d", next);
  }

  private String buildInsuranceVoucherMemo(VehicleInsurance insurance) {
    String vehicleNo = emptyToNull(insurance.getVehicleNo());
    String contractNumber = emptyToNull(insurance.getContractNumber());

    if (vehicleNo != null && contractNumber != null) {
      return "보험등록 보험료 / 차량번호: " + vehicleNo + " / 계약번호: " + contractNumber;
    }
    if (vehicleNo != null) {
      return "보험등록 보험료 / 차량번호: " + vehicleNo;
    }
    if (contractNumber != null) {
      return "보험등록 보험료 / 계약번호: " + contractNumber;
    }
    return "보험등록 보험료";
  }

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

  private VehicleInsuranceDtos.Response toRes(VehicleInsurance i) {
    return VehicleInsuranceDtos.Response.builder()
      .id(i.getId())
      .vehicleOrderId(i.getVehicleOrderId())
      .vehicleMgmtNo(i.getVehicleMgmtNo())
      .vehicleNo(i.getVehicleNo())
      .contractNumber(i.getContractNumber())
      .insuranceStartDate(i.getInsuranceStartDate())
      .insuranceEndDate(i.getInsuranceEndDate())
      .bodilyInjury(i.getBodilyInjury())
      .propertyDamage(i.getPropertyDamage())
      .personalInjury(i.getPersonalInjury())
      .autoInjury(i.getAutoInjury())
      .uninsured(i.getUninsured())
      .ownVehicle(i.getOwnVehicle())
      .emergency(i.getEmergency())
      .ageRange(i.getAgeRange())
      .driverRange(i.getDriverRange())
      .specialTerms(i.getSpecialTerms())
      .insurancePremium(i.getInsurancePremium())
      .createdAt(i.getCreatedAt())
      .updatedAt(i.getUpdatedAt())
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