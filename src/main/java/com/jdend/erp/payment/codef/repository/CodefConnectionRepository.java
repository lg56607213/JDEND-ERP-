package com.jdend.erp.payment.codef.repository;

import com.jdend.erp.payment.codef.entity.CodefConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodefConnectionRepository extends JpaRepository<CodefConnection, Long> {
    List<CodefConnection> findByActiveTrue();
    boolean existsByOrganizationAndAccountNo(String organization, String accountNo);
}
