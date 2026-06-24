package com.jdend.erp.vehicle.repository;

import com.jdend.erp.vehicle.entity.VehicleLoanVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleLoanVoucherRepository extends JpaRepository<VehicleLoanVoucher, Long> {

    List<VehicleLoanVoucher> findTop6ByLoan_IdOrderByVoucherDateDescIdDesc(Long loanId);

    List<VehicleLoanVoucher> findByLoan_IdOrderByVoucherDateAscIdAsc(Long loanId);
}