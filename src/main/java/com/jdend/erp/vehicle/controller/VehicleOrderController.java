package com.jdend.erp.vehicle.controller;

import com.jdend.erp.vehicle.dto.*;
import com.jdend.erp.vehicle.service.VehicleOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicle-orders")

public class VehicleOrderController {

    private final VehicleOrderService service;

    // 목록/검색
    @GetMapping
    public List<VehicleOrderResponse> list(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status
    ) {
        LocalDate s = (startDate == null || startDate.isBlank()) ? null : LocalDate.parse(startDate);
        LocalDate e = (endDate == null || endDate.isBlank()) ? null : LocalDate.parse(endDate);
        return service.search(s, e, status);
    }

    // 등록
    @PostMapping
    public VehicleOrderResponse create(@RequestBody VehicleOrderRequest req) {
        return service.create(req);
    }

    // 상세
    @GetMapping("/{mgmtNo}")
    public VehicleOrderResponse detail(@PathVariable String mgmtNo) {
        return service.detail(mgmtNo);
    }

    // 수정
    @PutMapping("/{mgmtNo}")
    public VehicleOrderResponse update(@PathVariable String mgmtNo, @RequestBody VehicleOrderRequest req) {
        return service.update(mgmtNo, req);
    }

    // 실행하기
    @PostMapping("/{mgmtNo}/execute")
    public VehicleDeliveryExecuteResponse execute(
            @PathVariable String mgmtNo,
            @RequestBody VehicleDeliveryExecuteRequest req
    ) {
        return service.executeDelivery(mgmtNo, req);
    }

    // 차량번호로 조회
    @GetMapping("/by-vehicle-no/{vehicleNo}")
    public VehicleLookupResponse lookupByVehicleNo(@PathVariable String vehicleNo) {
        return service.lookupByVehicleNo(vehicleNo);
    }
}