package com.jdend.erp.contract.service;

import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContractExpiryService {

    private final ContractRepository contractRepo;
    private final VehicleOrderRepository vehicleOrderRepo;

    private static final Set<String> TERMINATED = Set.of(
        "종료", "만기종료", "해지", "중도해지", "중도상환", "만기상환", "완료", "종결"
    );

    @Transactional
    public int expireForTenant(LocalDate today) {
        List<Contract> toExpire = contractRepo.findExpiredNotClosed(today, TERMINATED);

        for (Contract c : toExpire) {
            c.setStatus("종료");

            if (c.getVehicleNo() != null && !c.getVehicleNo().isBlank()) {
                vehicleOrderRepo.findByVehicleNoNormalized(c.getVehicleNo())
                    .ifPresent(v -> v.setOrderStatus("대기"));
            }
        }
        return toExpire.size();
    }
}
