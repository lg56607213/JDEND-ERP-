package com.jdend.erp.vehicle.fine.repository;

import com.jdend.erp.vehicle.fine.entity.TrafficFineFax;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TrafficFineFaxRepository extends JpaRepository<TrafficFineFax, Long> {

  List<TrafficFineFax> findByFineIdOrderByRequestedAtDescIdDesc(Long fineId);

  List<TrafficFineFax> findByRequestedAtBetweenOrderByRequestedAtDescIdDesc(LocalDateTime from, LocalDateTime to);
}
