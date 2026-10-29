package com.jdend.erp.web.repository;

import com.jdend.erp.web.entity.WebRentalHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WebRentalHoldRepository extends JpaRepository<WebRentalHold, Long> {

  Optional<WebRentalHold> findByHoldNo(String holdNo);

  // 기간 [from, to) 와 겹치는 가예약/확정 건이 잡고 있는 차량 id
  @Query("""
    select distinct h.vehicleOrder.id
    from WebRentalHold h
    where h.vehicleOrder is not null
      and h.status in :statuses
      and h.pickupAt < :to
      and h.returnAt > :from
  """)
  List<Long> findBusyVehicleIds(
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("statuses") Collection<String> statuses
  );

  @Query("""
    select h
    from WebRentalHold h
    where (:status = '' or h.status = :status)
      and (:fromAt is null or h.returnAt > :fromAt)
    order by h.pickupAt desc, h.id desc
  """)
  List<WebRentalHold> search(
      @Param("status") String status,
      @Param("fromAt") LocalDateTime fromAt
  );
}
