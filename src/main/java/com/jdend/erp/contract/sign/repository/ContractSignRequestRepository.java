package com.jdend.erp.contract.sign.repository;

import com.jdend.erp.contract.sign.entity.ContractSignRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractSignRequestRepository extends JpaRepository<ContractSignRequest, Long> {

    Optional<ContractSignRequest> findByTokenHash(String tokenHash);

    List<ContractSignRequest> findByTenantDbAndContractNumberOrderByIdDesc(String tenantDb, String contractNumber);
}
