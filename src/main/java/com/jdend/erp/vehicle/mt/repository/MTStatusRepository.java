package com.jdend.erp.vehicle.mt.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class MTStatusRepository {

  @PersistenceContext
  private EntityManager em;

  public List<Object[]> search(String vehicleMgmtNo, String vehicleNo,
                               LocalDate startDate, LocalDate endDate) {

    String mgmt = vehicleMgmtNo == null ? "" : vehicleMgmtNo.trim();
    String vno  = vehicleNo == null ? "" : vehicleNo.trim();

    String sql = """
      select
        m.id as maintenanceId,
        i.id as itemId,
        m.vehicle_mgmt_no as vehicleMgmtNo,
        m.vehicle_no as vehicleNo,

        c.start_date as contractStart,
        c.end_date as contractEnd,

        i.pay_date as retrieveDate,
        i.vendor as vendor,
        i.description as maintenanceType,
        i.amount as mtCost
      from vehicle_maintenance_items i
      join vehicle_maintenances m on m.id = i.maintenance_id
      left join contracts c
        on replace(replace(trim(c.vehicle_no), ' ', ''), '-', '') =
           replace(replace(trim(m.vehicle_no), ' ', ''), '-', '')
      where
        (:mgmt = '' or m.vehicle_mgmt_no like concat('%', :mgmt, '%'))
        and (:vno  = '' or m.vehicle_no like concat('%', :vno, '%'))
        and (:startDate is null or c.start_date >= :startDate)
        and (:endDate   is null or c.end_date   <= :endDate)
      order by i.id desc
      limit 500
    """;

    @SuppressWarnings("unchecked")
    List<Object[]> rows = em.createNativeQuery(sql)
        .setParameter("mgmt", mgmt)
        .setParameter("vno", vno)
        .setParameter("startDate", startDate)
        .setParameter("endDate", endDate)
        .getResultList();

    return rows;
  }
}