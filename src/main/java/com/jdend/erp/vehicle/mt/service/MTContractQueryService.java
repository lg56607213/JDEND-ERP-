package com.jdend.erp.vehicle.mt.service;

import com.jdend.erp.vehicle.mt.dto.MTContractSearchRowResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MTContractQueryService {

  @PersistenceContext
  private EntityManager em;

  @Transactional(readOnly = true)
  public List<MTContractSearchRowResponse> search(String keyword) {
    String kw = (keyword == null) ? "" : keyword.trim();

    String sql = """
      select
        c.contract_number as contractNumber,
        c.vehicle_no as vehicleNo,
        cu.customer_name as customerName,
        c.start_date as startDate,
        c.end_date as endDate,
        vo.vehicle_mgmt_no as vehicleMgmtNo
      from contracts c
      left join customers cu on cu.id = c.customer_id
      left join vehicle_orders vo
        on replace(replace(trim(vo.vehicle_no), ' ', ''), '-', '') =
           replace(replace(trim(c.vehicle_no),  ' ', ''), '-', '')
      where (:kw = '' or
        c.contract_number like concat('%', :kw, '%') or
        c.vehicle_no like concat('%', :kw, '%') or
        cu.customer_name like concat('%', :kw, '%')
      )
      order by c.id desc
      limit 200
    """;

    @SuppressWarnings("unchecked")
    List<Object[]> rows = em.createNativeQuery(sql)
        .setParameter("kw", kw)
        .getResultList();

    List<MTContractSearchRowResponse> out = new ArrayList<>();
    for (Object[] r : rows) {
      out.add(MTContractSearchRowResponse.builder()
          .contractNumber(toStr(r[0]))
          .vehicleNo(toStr(r[1]))
          .customerName(toStr(r[2]))
          .startDate(toLocalDate(r[3]))
          .endDate(toLocalDate(r[4]))
          .vehicleMgmtNo(toStr(r[5]))
          .build());
    }
    return out;
  }

  private String toStr(Object v) {
    return v == null ? "" : String.valueOf(v);
  }

  private LocalDate toLocalDate(Object v) {
    if (v == null) return null;
    if (v instanceof LocalDate ld) return ld;
    if (v instanceof Date d) return d.toLocalDate();
    return LocalDate.parse(String.valueOf(v));
  }
}