package com.jdend.erp.partner;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartnerRepository extends JpaRepository<Partner, Long> {

    Optional<Partner> findByPartnerNumber(String partnerNumber);

    @Query("SELECT MAX(p.partnerNumber) FROM Partner p")
    String findMaxPartnerNumber();

    List<Partner> findTop200ByOrderByIdDesc();

    @Query("""
        SELECT p
        FROM Partner p
        WHERE (:kw = '' OR
            p.partnerNumber LIKE CONCAT('%', :kw, '%') OR
            p.customerName LIKE CONCAT('%', :kw, '%') OR
            p.phone LIKE CONCAT('%', :kw, '%'))
        ORDER BY p.id DESC
    """)
    List<Partner> searchTop200(@Param("kw") String kw);

    List<Partner> findByCustomerNameContaining(String name);
}
