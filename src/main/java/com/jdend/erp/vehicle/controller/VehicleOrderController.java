package com.jdend.erp.vehicle.controller;

import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.common.excel.ExcelExportService;
import com.jdend.erp.common.excel.ExcelUploadResultResponse;
import com.jdend.erp.vehicle.dto.*;
import com.jdend.erp.vehicle.service.VehicleOrderBulkUploadService;
import com.jdend.erp.vehicle.service.VehicleOrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicle-orders")

public class VehicleOrderController {

    private final VehicleOrderService service;
    private final VehicleOrderBulkUploadService bulkUploadService;
    private final PermissionService permissionService;
    private final ExcelExportService excelExportService;

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

    // 등록 (1발주 N대 → 생성된 차량행 목록 반환)
    @PostMapping
    public List<VehicleOrderResponse> create(@RequestBody VehicleOrderRequest req) {
        return service.create(req);
    }

    // 상세 (차량관리번호 기반 — 실행 후 유니크 번호 조회용)
    @GetMapping("/{mgmtNo}")
    public VehicleOrderResponse detail(@PathVariable String mgmtNo) {
        return service.detail(mgmtNo);
    }

    // 상세 (행 PK 기반 — 발주~선급 pre-실행 단계, …000 공유값 대비)
    @GetMapping("/by-id/{id}")
    public VehicleOrderResponse detailById(@PathVariable Long id) {
        return service.detailById(id);
    }

    // 수정 (차량관리번호 기반)
    @PutMapping("/{mgmtNo}")
    public VehicleOrderResponse update(@PathVariable String mgmtNo, @RequestBody VehicleOrderRequest req, HttpSession session) {
        permissionService.requireManager(session);
        return service.update(mgmtNo, req);
    }

    // 수정 (행 PK 기반 — 발주~선급 pre-실행 단계)
    @PutMapping("/by-id/{id}")
    public VehicleOrderResponse updateById(@PathVariable Long id, @RequestBody VehicleOrderRequest req, HttpSession session) {
        permissionService.requireManager(session);
        return service.updateById(id, req);
    }

    // 차량 등록 (행 PK 기반 — …000 공유값 대비, S3 실번호 확정 포함)
    @PostMapping("/by-id/{id}/register")
    public VehicleOrderResponse registerById(
            @PathVariable Long id,
            @RequestPart(value = "data") VehicleRegisterRequest req,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpSession session) {
        permissionService.requireManager(session);
        return service.registerVehicleById(id, req, file);
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

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status
    ) {
        LocalDate s = (startDate == null || startDate.isBlank()) ? null : LocalDate.parse(startDate);
        LocalDate e = (endDate == null || endDate.isBlank()) ? null : LocalDate.parse(endDate);
        String[] headers = {"차량관리번호", "발주상태", "제작사계약번호", "차종", "옵션명",
                "차량가격", "옵션가격", "총차량가", "발주일자", "차대번호",
                "출고가", "총선급가액", "차량번호", "등록일자", "연식", "유종", "배기량"};
        List<Object[]> rows = service.search(s, e, status).stream().map(v -> new Object[]{
                v.getVehicleMgmtNo(), v.getOrderStatus(), v.getMakerContractNo(), v.getCarModel(), v.getOptionName(),
                v.getVehiclePrice(), v.getOptionPrice(), v.getTotalPrice(), v.getOrderDate(), v.getChassisNo(),
                v.getReleasePrice(), v.getTotalAdvancePrice(), v.getVehicleNo(), v.getRegisterDate(),
                v.getModelYear(), v.getFuelType(), v.getDisplacement()
        }).collect(Collectors.toList());
        byte[] data = excelExportService.build("차량목록", headers, rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''vehicles.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    // 차량상태 (계약/대기/매각) 목록
    @GetMapping("/state")
    public List<VehicleStateResponse> state() {
        return service.vehicleStateList();
    }

    @GetMapping("/bulk-upload/template")
    public ResponseEntity<byte[]> bulkUploadTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vehicle_order_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bulkUploadService.template());
    }

    @PostMapping("/bulk-upload")
    public ExcelUploadResultResponse bulkUpload(@RequestParam("file") MultipartFile file) {
        return bulkUploadService.upload(file);
    }
}