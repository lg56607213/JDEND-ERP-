package com.jdend.erp.myinfo.repository;

import com.jdend.erp.myinfo.entity.CorporateCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CorporateCardRepository extends JpaRepository<CorporateCard, Long> {
    List<CorporateCard> findAllByOrderByIdAsc();
    List<CorporateCard> findByIsActiveTrueOrderByIdAsc();
}
