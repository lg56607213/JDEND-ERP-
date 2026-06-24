package com.jdend.erp.dashboard.repository;

import com.jdend.erp.dashboard.dto.DashboardReceivableRow;
import com.jdend.erp.payment.receivable.entity.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReceivableDashboardRepository extends JpaRepository<Receivable, Long> {

  // ✅ status 기본값이 "미납" 이라서, 완료만 제외
  @Query("""
    select new com.jdend.erp.dashboard.dto.DashboardReceivableRow(
      r.contractNumber,
      r.customerName,
      coalesce(r.receivableAmount,0),
      0
    )
    from Receivable r
    where (r.status is null or r.status <> '완료')
    order by coalesce(r.receivableAmount,0) desc
  """)
  List<DashboardReceivableRow> findTopReceivablesRaw();

  default List<DashboardReceivableRow> findTopReceivables(int limit) {
    List<DashboardReceivableRow> all = findTopReceivablesRaw();
    if (all.size() <= limit) return all;
    return all.subList(0, limit);
  }
}