package com.jdend.erp.accounting.depreciation.service;

import com.jdend.erp.accounting.depreciation.dto.*;
import com.jdend.erp.accounting.depreciation.entity.*;
import com.jdend.erp.accounting.depreciation.repository.*;
import com.jdend.erp.accounting.voucher.dto.VoucherCreateRequest;
import com.jdend.erp.accounting.voucher.dto.VoucherCreateResponse;
import com.jdend.erp.accounting.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepreciationService {

  private final DepreciationAssetRepository assetRepo;
  private final DepreciationScheduleLineRepository lineRepo;
  private final DepreciationPostingRepository postingRepo;
  private final VoucherService voucherService;

  @Transactional
  public Long createAsset(DepreciationAssetCreateRequest req) {
    if (req.vehicleNo == null || req.vehicleNo.trim().isEmpty()) {
      throw new IllegalArgumentException("차량번호는 필수입니다.");
    }
    if (req.acquisitionCost == null || req.acquisitionCost <= 0) {
      throw new IllegalArgumentException("최초취득가액은 1 이상이어야 합니다.");
    }
    if (req.depStartDate == null || req.depEndDate == null) {
      throw new IllegalArgumentException("감가상각 시작/종료일은 필수입니다.");
    }
    if (req.depEndDate.isBefore(req.depStartDate)) {
      throw new IllegalArgumentException("감가상각 종료일은 시작일보다 빠를 수 없습니다.");
    }

    int months = monthsInclusive(req.depStartDate, req.depEndDate);
    long monthly = Math.round((double) req.acquisitionCost / (double) months);

    DepreciationAsset asset = DepreciationAsset.builder()
      .vehicleMgmtNo(nz(req.vehicleMgmtNo))
      .vehicleNo(req.vehicleNo.trim())
      .contractNumber(nz(req.contractNumber))
      .acquisitionCost(req.acquisitionCost)
      .depMethod(nz(req.depMethod))
      .assetType(nz(req.assetType))
      .depStartDate(req.depStartDate)
      .depEndDate(req.depEndDate)
      .totalMonths(months)
      .monthlyAmount(monthly)
      .active(true)
      .startDate(req.depStartDate)
      .build();

    asset = assetRepo.save(asset);

    List<DepreciationScheduleLine> lines = generateStraightLine(asset, 1, asset.getMonthlyAmount(), null, null, null);
    lineRepo.saveAll(lines);

    return asset.getId();
  }

  @Transactional(readOnly = true)
  public List<DepreciationAssetRowResponse> listAssets(
      String baseMonth,
      String depMethod,
      String assetType,
      String vehicleNo,
      LocalDate depStartFrom,
      LocalDate depEndTo
  ) {
    YearMonth ym = (baseMonth == null || baseMonth.isBlank())
        ? YearMonth.now()
        : YearMonth.parse(baseMonth);

    LocalDate asOf = ym.atEndOfMonth();
    LocalDate monthStart = ym.atDay(1);
    LocalDate monthEnd = ym.atEndOfMonth();

    List<DepreciationAsset> all = assetRepo.findAll().stream()
        .filter(a -> Boolean.TRUE.equals(a.getActive()))
        .collect(Collectors.toList());

    if (depMethod != null && !depMethod.isBlank()) {
      all = all.stream().filter(a -> depMethod.equals(a.getDepMethod())).toList();
    }
    if (assetType != null && !assetType.isBlank()) {
      all = all.stream().filter(a -> assetType.equals(a.getAssetType())).toList();
    }
    if (vehicleNo != null && !vehicleNo.isBlank()) {
      String kw = vehicleNo.trim();
      all = all.stream().filter(a -> a.getVehicleNo() != null && a.getVehicleNo().contains(kw)).toList();
    }
    if (depStartFrom != null) {
      all = all.stream().filter(a -> a.getDepStartDate() != null && !a.getDepStartDate().isBefore(depStartFrom)).toList();
    }
    if (depEndTo != null) {
      all = all.stream().filter(a -> a.getDepEndDate() != null && !a.getDepEndDate().isAfter(depEndTo)).toList();
    }

    List<DepreciationAssetRowResponse> rows = new ArrayList<>();

    for (DepreciationAsset a : all) {
      int ver = lineRepo.findMaxVersion(a.getId());
      if (ver == 0) ver = 1;

      DepreciationScheduleLine latest = lineRepo.findLatestLineUpTo(a.getId(), ver, asOf)
          .stream().findFirst().orElse(null);

      long remaining = (latest == null) ? a.getAcquisitionCost() : latest.getBalance();
      long accumulated = a.getAcquisitionCost() - remaining;
      String lastDepDate = (latest == null || latest.getDepreciationDate() == null) ? "" : latest.getDepreciationDate().toString();

      String voucherDate = postingRepo.findByAsset_IdAndBaseMonth(a.getId(), ym.toString())
          .map(p -> p.getVoucherDate().toString())
          .orElse("");

      int totalRounds = lineRepo.findMaxPeriodNo(a.getId(), ver);
      int postedRounds = (int) postingRepo.countByAssetId(a.getId());

      Integer currentRound = lineRepo.findLinesInMonth(a.getId(), ver, monthStart, monthEnd)
          .stream()
          .findFirst()
          .map(DepreciationScheduleLine::getPeriodNo)
          .orElse(null);

      boolean currentRoundPosted = postingRepo.findByAsset_IdAndBaseMonth(a.getId(), ym.toString()).isPresent();

      rows.add(DepreciationAssetRowResponse.builder()
        .id(a.getId())
        .vehicleMgmtNo(nz(a.getVehicleMgmtNo()))
        .vehicleNo(nz(a.getVehicleNo()))
        .acquisitionCost(a.getAcquisitionCost())
        .depStartDate(a.getDepStartDate() == null ? "" : a.getDepStartDate().toString())
        .depEndDate(a.getDepEndDate() == null ? "" : a.getDepEndDate().toString())
        .monthlyAmount(a.getMonthlyAmount())
        .accumulated(accumulated)
        .remaining(remaining)
        .lastDepDate(lastDepDate)
        .voucherDate(voucherDate)
        .totalRounds(totalRounds)
        .postedRounds(postedRounds)
        .currentRound(currentRound)
        .currentRoundPosted(currentRoundPosted)
        .build());
    }

    rows.sort(Comparator.comparing((DepreciationAssetRowResponse r) -> r.id).reversed());
    return rows;
  }

  @Transactional(readOnly = true)
  public DepreciationAsset getAsset(Long id) {
    return assetRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("자산이 없습니다. id=" + id));
  }

  @Transactional(readOnly = true)
  public List<ScheduleLineResponse> getSchedule(Long assetId) {
    int ver = lineRepo.findMaxVersion(assetId);
    if (ver == 0) ver = 1;
    return toScheduleResponse(lineRepo.findByAsset_IdAndVersionNoOrderByPeriodNoAsc(assetId, ver));
  }

  @Transactional
  public List<ScheduleLineResponse> changeSchedule(Long assetId, ScheduleChangeRequest req) {
    DepreciationAsset asset = getAsset(assetId);

    if (req.changeType == null || req.changeType.isBlank()) throw new IllegalArgumentException("변경유형은 필수입니다.");
    if (req.changeDate == null) throw new IllegalArgumentException("변경적용일은 필수입니다.");

    int currentVer = lineRepo.findMaxVersion(assetId);
    if (currentVer == 0) currentVer = 1;
    int newVer = currentVer + 1;

    String type = req.changeType.trim();
    Long monthly = asset.getMonthlyAmount();
    Integer totalMonths = asset.getTotalMonths();
    LocalDate start = asset.getStartDate();

    if ("extend".equals(type)) {
      int ext = (req.extendMonths == null ? 0 : req.extendMonths);
      if (ext <= 0) throw new IllegalArgumentException("기간연장 개월수(extendMonths)는 1 이상이어야 합니다.");
      totalMonths = totalMonths + ext;

      asset.setTotalMonths(totalMonths);
      asset.setDepEndDate(start.plusMonths(totalMonths - 1));
      asset = assetRepo.save(asset);
    }

    List<DepreciationScheduleLine> lines;

    if ("amount".equals(type)) {
      if (req.newMonthlyAmount == null || req.newMonthlyAmount <= 0) {
        throw new IllegalArgumentException("변경후 상각액은 1 이상이어야 합니다.");
      }
      lines = generateStraightLine(asset, newVer, monthly, "amount", req.changeDate, req.newMonthlyAmount);
    } else if ("sale".equals(type)) {
      if (req.saleAmount == null || req.saleAmount < 0) throw new IllegalArgumentException("매각금액은 0 이상이어야 합니다.");
      lines = generateStraightLine(asset, newVer, monthly, "sale", req.changeDate, req.saleAmount);
    } else if ("terminate".equals(type)) {
      lines = generateStraightLine(asset, newVer, monthly, "terminate", req.changeDate, 0L);
    } else if ("extend".equals(type)) {
      lines = generateStraightLine(asset, newVer, monthly, null, null, null);
    } else {
      throw new IllegalArgumentException("지원하지 않는 변경유형입니다: " + type);
    }

    lineRepo.saveAll(lines);
    return toScheduleResponse(lines);
  }

  @Transactional
  public Map<String, Object> postDepreciation(PostDepreciationRequest req) {
    if (req.baseMonth == null || req.baseMonth.isBlank()) throw new IllegalArgumentException("기준월은 필수입니다.");
    if (req.voucherDate == null) throw new IllegalArgumentException("전표발생일은 필수입니다.");
    if (req.assetIds == null || req.assetIds.isEmpty()) throw new IllegalArgumentException("자산 선택이 필요합니다.");

    YearMonth ym = YearMonth.parse(req.baseMonth);
    LocalDate monthStart = ym.atDay(1);
    LocalDate monthEnd = ym.atEndOfMonth();

    int postingSavedCount = 0;
    int voucherCreatedCount = 0;
    int skippedCount = 0;

    List<String> processedRounds = new ArrayList<>();
    List<String> voucherNos = new ArrayList<>();

    for (Long assetId : req.assetIds) {
      DepreciationAsset asset = getAsset(assetId);

      int ver = lineRepo.findMaxVersion(assetId);
      if (ver == 0) ver = 1;

      DepreciationScheduleLine currentLine = lineRepo.findLinesInMonth(assetId, ver, monthStart, monthEnd)
          .stream()
          .findFirst()
          .orElse(null);

      if (currentLine == null || currentLine.getPeriodNo() == null || currentLine.getPeriodNo() <= 0) {
        skippedCount++;
        continue;
      }

      if (currentLine.getAmount() == null || currentLine.getAmount() <= 0) {
        skippedCount++;
        continue;
      }

      if (postingRepo.findByAsset_IdAndBaseMonth(assetId, req.baseMonth).isPresent()) {
        skippedCount++;
        continue;
      }

      DepreciationPosting posting = DepreciationPosting.builder()
          .asset(asset)
          .baseMonth(req.baseMonth)
          .voucherDate(req.voucherDate)
          .build();
      postingRepo.save(posting);
      postingSavedCount++;

      VoucherCreateRequest voucherReq = VoucherCreateRequest.builder()
          .voucherDate(req.voucherDate)
          .contractNumber(blankToNull(asset.getContractNumber()))
          .vehicleNo(blankToNull(asset.getVehicleNo()))
          .memo(String.format("감가상각 %s %d회차", req.baseMonth, currentLine.getPeriodNo()))
          .debitEntries(List.of(
              VoucherCreateRequest.VoucherLineRequest.builder()
                  .account("감가상각비")
                  .amount(currentLine.getAmount())
                  .description(String.format("%s %d회차 감가상각", nz(asset.getVehicleNo()), currentLine.getPeriodNo()))
                  .build()
          ))
          .creditEntries(List.of(
              VoucherCreateRequest.VoucherLineRequest.builder()
                  .account("감가상각누계액")
                  .amount(currentLine.getAmount())
                  .description(String.format("%s %d회차 감가상각", nz(asset.getVehicleNo()), currentLine.getPeriodNo()))
                  .build()
          ))
          .build();

      VoucherCreateResponse voucherRes = voucherService.create(voucherReq);
      voucherCreatedCount++;

      processedRounds.add(String.format("%s / %d회차 / %,d원", nz(asset.getVehicleNo()), currentLine.getPeriodNo(), currentLine.getAmount()));
      voucherNos.add(voucherRes.getVoucherNo());
    }

    return Map.of(
        "ok", true,
        "postingSavedCount", postingSavedCount,
        "voucherCreatedCount", voucherCreatedCount,
        "skippedCount", skippedCount,
        "processedRounds", processedRounds,
        "voucherNos", voucherNos
    );
  }

  @Transactional
  public void completeAssets(List<Long> assetIds) {
    if (assetIds == null || assetIds.isEmpty()) throw new IllegalArgumentException("선택이 필요합니다.");
    for (Long id : assetIds) {
      DepreciationAsset a = getAsset(id);
      a.setActive(false);
      assetRepo.save(a);
    }
  }

  private List<DepreciationScheduleLine> generateStraightLine(
      DepreciationAsset asset,
      int version,
      long baseMonthlyAmount,
      String changeType,
      LocalDate changeDate,
      Long changeValue
  ) {
    List<DepreciationScheduleLine> lines = new ArrayList<>();

    long acquisition = asset.getAcquisitionCost();
    LocalDate start = asset.getStartDate();
    int totalMonths = asset.getTotalMonths();

    lines.add(DepreciationScheduleLine.builder()
      .asset(asset)
      .versionNo(version)
      .periodNo(0)
      .depreciationDate(null)
      .amount(0L)
      .balance(acquisition)
      .note("")
      .build());

    long balance = acquisition;

    for (int i = 1; i <= totalMonths; i++) {
      LocalDate depDate = start.plusMonths(i - 1);

      long amount = baseMonthlyAmount;
      String note = "";

      boolean isFinalPlainPeriod = (i == totalMonths)
          && (changeType == null || changeDate == null || depDate.isBefore(changeDate));

      if (isFinalPlainPeriod) {
        // 월 상각액은 취득원가/개월수를 반올림한 값이라 매달 곱하면 취득원가와 몇 원씩
        // 어긋난다. 마지막 회차에는 남은 잔액을 전부 상각해 합계가 취득원가와 정확히
        // 일치하도록 맞춘다(매각/조기종료 등 변경 이벤트가 없을 때만 적용).
        amount = balance;
      }

      if (changeType != null && changeDate != null) {
        if (!depDate.isBefore(changeDate)) {
          if ("sale".equals(changeType)) {
            if (depDate.equals(changeDate)) {
              balance = Math.max(0L, changeValue);
              amount = 0L;
              note = "매각";
            } else {
              amount = 0L;
              balance = 0L;
              note = "";
            }
          } else if ("terminate".equals(changeType)) {
            if (depDate.equals(changeDate)) {
              amount = 0L;
              note = "조기종료";
            } else {
              amount = 0L;
              balance = 0L;
              note = "";
            }
          } else if ("amount".equals(changeType)) {
            amount = Math.max(0L, changeValue);
            if (depDate.equals(changeDate)) note = "상각액변경";
          }
        }
      }

      if (!"sale".equals(changeType) && !"terminate".equals(changeType)) {
        balance = balance - amount;
        if (balance < 0) balance = 0;
      }

      lines.add(DepreciationScheduleLine.builder()
        .asset(asset)
        .versionNo(version)
        .periodNo(i)
        .depreciationDate(depDate)
        .amount(amount)
        .balance(balance)
        .note(note)
        .build());
    }

    return lines;
  }

  private List<ScheduleLineResponse> toScheduleResponse(List<DepreciationScheduleLine> lines) {
    return lines.stream().map(l -> ScheduleLineResponse.builder()
      .period(l.getPeriodNo())
      .date(l.getDepreciationDate() == null ? "" : l.getDepreciationDate().toString())
      .amount(l.getAmount())
      .balance(l.getBalance())
      .note(l.getNote() == null ? "" : l.getNote())
      .build()).toList();
  }

  private static int monthsInclusive(LocalDate start, LocalDate end) {
    int months = (end.getYear() - start.getYear()) * 12 + (end.getMonthValue() - start.getMonthValue()) + 1;
    return Math.max(months, 1);
  }

  private static String nz(String s) {
    return (s == null) ? "" : s;
  }

  private static String blankToNull(String s) {
    return (s == null || s.isBlank()) ? null : s.trim();
  }
}