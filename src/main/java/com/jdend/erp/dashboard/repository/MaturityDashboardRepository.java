package com.jdend.erp.dashboard.repository;

import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.dashboard.dto.DashboardMaturityRow;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MaturityDashboardRepository extends JpaRepository<Contract, Long> {

  @Query("""
    select new com.jdend.erp.dashboard.dto.DashboardMaturityRow(
      c.contractNumber,
      c.customer.customerName,
      c.endDate,
      0
    )
    from Contract c
    where c.endDate between :from and :to
      and (c.status is null or trim(c.status) not in ('해지', '만기종료', '종료'))
    order by c.endDate asc
  """)
  List<DashboardMaturityRow> findMaturitySoonRaw(@Param("from") LocalDate from,
                                                 @Param("to") LocalDate to);

  default List<DashboardMaturityRow> findMaturitySoon(LocalDate from, LocalDate to, int limit) {
    List<DashboardMaturityRow> all = findMaturitySoonRaw(from, to);
    if (all.size() <= limit) return all;
    return all.subList(0, limit);
  }
}