package com.jdend.erp.vehicle.mt.service;

import com.jdend.erp.vehicle.mt.dto.MTStatusRowResponse;
import com.jdend.erp.vehicle.mt.repository.MTStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MTStatusService {

  private final MTStatusRepository repo;

  @Transactional(readOnly = true)
  public List<MTStatusRowResponse> search(String vehicleMgmtNo, String vehicleNo,
                                         LocalDate startDate, LocalDate endDate) {

    String mgmt = vehicleMgmtNo == null ? "" : vehicleMgmtNo.trim();
    String vno  = vehicleNo == null ? "" : vehicleNo.trim();

    List<Object[]> rows = repo.search(mgmt, vno, startDate, endDate);

    List<MTStatusRowResponse> out = new ArrayList<>();
    for (Object[] r : rows) {
      out.add(MTStatusRowResponse.builder()
          .maintenanceId(toLong(r[0]))
          .itemId(toLong(r[1]))
          .vehicleMgmtNo(toStr(r[2]))
          .vehicleNo(toStr(r[3]))
          .contractStart(toLocalDate(r[4]))
          .contractEnd(toLocalDate(r[5]))
          .retrieveDate(toLocalDate(r[6]))
          .vendor(toStr(r[7]))
          .maintenanceType(toStr(r[8]))
          .mtCost(toLong(r[9]) == null ? 0L : toLong(r[9]))
          .build());
    }
    return out;
  }

  private String toStr(Object v) {
    return v == null ? "" : String.valueOf(v);
  }

  private Long toLong(Object v) {
    if (v == null) return null;
    if (v instanceof Number n) return n.longValue();
    return Long.parseLong(String.valueOf(v));
  }

  private LocalDate toLocalDate(Object v) {
    if (v == null) return null;
    if (v instanceof LocalDate ld) return ld;
    if (v instanceof Date d) return d.toLocalDate();
    return LocalDate.parse(String.valueOf(v));
  }
}