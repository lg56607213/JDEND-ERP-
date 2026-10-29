package com.jdend.erp.vehicle.controller;

import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.vehicle.dto.*;
import com.jdend.erp.vehicle.service.VehicleLoanService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicle-loans")
public class VehicleLoanController {

    private final VehicleLoanService service;
    private final PermissionService permissionService;

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

    // 전표 없이 기록된 상환(이관분)만 지운다 — 이관 데이터를 다시 넣을 때 사용 (책임자 이상)
    @DeleteMapping("/{id}/imported-repayments")
    public Map<String, Integer> deleteImportedRepayments(@PathVariable Long id, HttpSession session) {
        permissionService.requireManager(session);
        return Map.of("deleted", service.deleteImportedRepayments(id));
    }
}