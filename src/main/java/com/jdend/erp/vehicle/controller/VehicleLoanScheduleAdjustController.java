package com.jdend.erp.vehicle.controller;

import com.jdend.erp.vehicle.dto.LoanVehiclePickerRowDto;
import com.jdend.erp.vehicle.dto.VehicleLoanScheduleResponse;
import com.jdend.erp.vehicle.dto.VehicleLoanScheduleSaveRequest;
import com.jdend.erp.vehicle.service.VehicleLoanScheduleAdjustService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/loan-schedule-adjust")

public class VehicleLoanScheduleAdjustController {

    private final VehicleLoanScheduleAdjustService service;

    @GetMapping("/picker")
    public List<LoanVehiclePickerRowDto> picker(
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw
    ) {
        return service.searchLoanVehiclePicker(kw);
    }

    @GetMapping("/by-vehicle-no/{vehicleNo}")
    public VehicleLoanScheduleResponse getByVehicleNo(@PathVariable String vehicleNo) {
        return service.getByVehicleNo(vehicleNo);
    }

    @PutMapping
    public Map<String, Object> save(@RequestBody VehicleLoanScheduleSaveRequest req) {
        int savedCount = service.saveAdjustedSchedule(req);
        return Map.of(
                "message", "차입금 스케줄이 저장되었습니다.",
                "savedCount", savedCount
        );
    }
}