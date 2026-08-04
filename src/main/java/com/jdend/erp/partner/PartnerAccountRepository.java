package com.jdend.erp.partner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerAccountRepository extends JpaRepository<PartnerAccount, Long> {
    List<PartnerAccount> findByPartnerId(Long partnerId);
    List<PartnerAccount> findByPartnerNumber(String partnerNumber);
}
