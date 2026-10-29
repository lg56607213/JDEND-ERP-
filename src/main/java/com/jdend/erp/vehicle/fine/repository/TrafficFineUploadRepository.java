package com.jdend.erp.vehicle.fine.repository;

import com.jdend.erp.vehicle.fine.entity.TrafficFineUpload;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrafficFineUploadRepository extends JpaRepository<TrafficFineUpload, Long> {

  List<TrafficFineUpload> findAllByOrderByUploadedAtDescIdDesc(Pageable pageable);
}
