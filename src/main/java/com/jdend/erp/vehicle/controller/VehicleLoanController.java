package com.jdend.erp.vehicle.controller;

import com.jdend.erp.vehicle.dto.*;
import com.jdend.erp.vehicle.service.VehicleLoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicle-loans")
public class VehicleLoanController {

    private final VehicleLoanService service;

    @GetMapping
    public List<VehicleLoanListItemResponse> list(
            @RequestParam(required = false, defaultValue = "") String contractNo,
            @RequestParam(required = false, defaultValue = "") String vehicleNo,
            @RequestParam(required = false, defaultValue = "") String financeName,
            @RequestParam(required = false) Boolean terminated
    ) {
        return service.list(contractNo, vehicleNo, financeName, terminated);
    }

    @GetMapping("/{id}")
    public VehicleLoanDetailResponse detail(@PathVariable Long id) {
        return service.detail(id);
    }

    // 차입금 상환 현황
    @GetMapping("/{id}/repayment-status")
    public List<VehicleLoanRepaymentStatusRowResponse> repaymentStatus(@PathVariable Long id) {
        return service.repaymentStatus(id);
    }

    @GetMapping("/by-vehicle-no/{vehicleNo}")
    public VehicleLoanDetailResponse findByVehicleNo(@PathVariable String vehicleNo) {
        return service.findLatestByVehicleNo(vehicleNo);
    }

    @PostMapping
    public VehicleLoanListItemResponse create(@RequestBody VehicleLoanCreateRequest req) {
        return service.create(req);
    }

    @PatchMapping("/{id}")
    public VehicleLoanListItemResponse update(
            @PathVariable Long id,
            @RequestBody VehicleLoanUpdateRequest req
    ) {
        return service.update(id, req);
    }

    @PostMapping("/terminate")
    public void terminate(@RequestBody List<Long> loanIds) {
        service.terminate(loanIds);
    }

    @PostMapping("/vouchers")
    public void createVouchers(@RequestBody VehicleLoanVoucherCreateRequest req) {
        service.createVouchers(req);
    }
}