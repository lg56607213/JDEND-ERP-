package com.jdend.erp.accounting.voucher.controller;

import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounting/lookup/vehicle-orders")

public class VehicleSearchController {

    private final VehicleOrderRepository vehicleOrderRepo;

    // ✅ 회계/전표(돋보기)용 차량 검색
    // GET /api/accounting/lookup/vehicle-orders/search?kw=
    @GetMapping("/search")
    public List<VehicleSearchRow> search(
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw
    ) {
        String keyword = (kw == null) ? "" : kw.trim();

        List<VehicleOrder> list = vehicleOrderRepo.searchTop500(keyword);

        List<VehicleSearchRow> out = new ArrayList<>();
        for (VehicleOrder v : list) {
            out.add(new VehicleSearchRow(
                    v.getVehicleMgmtNo(),
                    v.getVehicleNo(),
                    v.getCarModel(),
                    v.getInspectionStart(),
                    v.getInspectionEnd()
            ));
        }
        return out;
    }

    @Getter
    @AllArgsConstructor
    public static class VehicleSearchRow {
        private String vehicleMgmtNo;
        private String vehicleNo;
        private String carModel;
        private LocalDate inspectionStart;
        private LocalDate inspectionEnd;
    }
}