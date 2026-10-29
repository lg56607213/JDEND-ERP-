package com.jdend.erp.vehicle.service;

import com.jdend.erp.accounting.voucher.service.VoucherService;
import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.customer.Customer;
import com.jdend.erp.document.service.VehicleDocumentService;
import com.jdend.erp.vehicle.dto.*;
import com.jdend.erp.vehicle.entity.*;
import com.jdend.erp.vehicle.repository.*;
import com.jdend.erp.vehicle.sale.repository.VehicleSaleRepository;
import com.jdend.erp.vehicle.service.VehicleLoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleOrderService {

    private final VehicleOrderRepository orderRepo;
    private final VehicleOrderHistoryRepository historyRepo;
    private final VoucherService voucherService;
    private final VehicleSaleRepository saleRepo;
    private final ContractRepository contractRepo;
    private final VehicleNumberGenerator numberGenerator;
    private final VehicleLoanService vehicleLoanService;
    private final com.jdend.erp.common.storage.FileStorage storage;
    private final VehicleDocumentService vehicleDocumentService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * 이미 처분해 보유하지 않는 차량(타 시스템 이관 이력 차량 등). 매각 기록(vehicle_sales) 없이도
     * 차량 상태를 매각으로 보이고 배차 가능 목록에서 뺀다.
     */
    public static final String SOLD_STATUS = "매각";

    /** 등록이 끝난 상태. 이 상태가 되면 차량관리번호를 확정한다. */
    private static final Set<String> REGISTERED_STATUSES = Set.of("등록완료", "실행완료");

    /** 차량 행 id(vehicle_order_id)를 함께 가진 테이블 — 번호 확정 시 id 기준으로 함께 바꾼다. */
    private static final List<String> MGMT_NO_TABLES_WITH_ORDER_ID = List.of(
            "vehicle_insurances", "vehicle_inspections", "vehicle_loans",
            "vehicle_sales", "vehicle_maintenances", "vehicle_dispatches");

    /** 차량 행 id 없이 차량관리번호만 가진 테이블 — 임시번호를 이 차량만 쓰고 있을 때만 바꾼다. */
    private static final List<String> MGMT_NO_TABLES_WITHOUT_ORDER_ID = List.of(
            "vehicle_documents", "depreciation_assets");

    @Transactional(readOnly = true)
    public List<VehicleSearchRowResponse> searchForPicker(String kw) {
        String q = (kw == null) ? "" : kw.trim();

        List<VehicleOrder> list = orderRepo.searchTop500(q);

        return list.stream().map(v -> VehicleSearchRowResponse.builder()
                .vehicleMgmtNo(v.getVehicleMgmtNo())
                .vehicleNo(v.getVehicleNo())
                .carModel(v.getCarModel())
                .makerContractNo(v.getMakerContractNo())
                .build()
        ).toList();
    }

    @Transactional(readOnly = true)
    public VehicleLookupResponse lookupByVehicleNo(String vehicleNo) {
        if (vehicleNo == null || vehicleNo.isBlank()) {
            throw new RuntimeException("차량번호(vehicleNo) 필수");
        }

        VehicleOrder o = orderRepo.findByVehicleNoNormalized(vehicleNo)
                .orElseThrow(() -> new RuntimeException("해당 차량번호를 찾을 수 없습니다: " + vehicleNo));

        return VehicleLookupResponse.builder()
                .vehicleNo(o.getVehicleNo())
                .vehicleMgmtNo(o.getVehicleMgmtNo())
                .orderStatus(o.getOrderStatus())
                .makerContractNo(o.getMakerContractNo())
                .carModel(o.getCarModel())
                .build();
    }

    @Transactional(readOnly = true)
    public List<VehicleOrderResponse> search(LocalDate start, LocalDate end, String status) {
        return search(start, end, status, null, null);
    }

    public List<VehicleOrderResponse> search(LocalDate start, LocalDate end, String status,
                                             LocalDate registerStart, LocalDate registerEnd) {
        List<VehicleOrder> list;

        boolean hasOrderDate    = (start != null && end != null);
        boolean hasRegisterDate = (registerStart != null && registerEnd != null);
        boolean hasStatus       = (status != null && !status.isBlank());

        if (hasRegisterDate && hasStatus) {
            list = orderRepo.findByOrderStatusAndRegisterDateBetween(status, registerStart, registerEnd);
        } else if (hasRegisterDate) {
            list = orderRepo.findByRegisterDateBetween(registerStart, registerEnd);
        } else if (hasOrderDate && hasStatus) {
            list = orderRepo.findByOrderStatusAndOrderDateBetween(status, start, end);
        } else if (hasOrderDate) {
            list = orderRepo.findByOrderDateBetween(start, end);
        } else if (hasStatus) {
            list = orderRepo.findByOrderStatus(status);
        } else {
            list = orderRepo.findAll();
        }

        list.sort(Comparator.comparing(VehicleOrder::getOrderDate).reversed());
        return list.stream().map(this::toSimple).toList();
    }

    @Transactional
    public List<VehicleOrderResponse> create(VehicleOrderRequest req) {
        if (req.carModel == null || req.carModel.isBlank()) {
            throw new RuntimeException("차종(carModel)은 필수");
        }
        if (req.vehiclePrice == null) {
            throw new RuntimeException("차량가격(vehiclePrice)은 필수");
        }

        int quantity = (req.quantity == null || req.quantity < 1) ? 1 : req.quantity;
        if (quantity > 99) {
            throw new RuntimeException("발주 대수는 1~99대만 가능합니다.");
        }

        LocalDate orderDate = (req.orderDate != null) ? req.orderDate : LocalDate.now();

        // 발주번호(10자리) 채번 — N대는 같은 발주번호를 공유(그룹 헤더)
        String orderNo = numberGenerator.nextOrderNo(orderDate);

        // Phase 2 v5: 발주(미실행) 차량관리번호 = 발주번호 + "000". N대 모두 동일값 공유.
        // 개별 식별은 행 PK(id)로 하며, 실번호(…001,…002)는 실행(S3)에서 확정한다.
        String mgmtNo = VehicleNumberGenerator.buildOrderStageMgmtNo(orderNo);

        List<VehicleOrderResponse> result = new ArrayList<>();

        for (int unit = 1; unit <= quantity; unit++) {
            VehicleOrder o = VehicleOrder.builder()
                    .vehicleMgmtNo(mgmtNo)
                    .orderNo(orderNo)
                    // BUG-1 수정: 요청에 orderStatus 가 있으면 그대로 사용, 없으면 "발주전" 기본값
                    .orderStatus(req.orderStatus != null && !req.orderStatus.isBlank()
                            ? req.orderStatus : "발주전")
                    .makerContractNo(pickMakerContractNo(req, unit))
                    .carModel(req.carModel)
                    .optionName(emptyToNull(req.optionName))
                    .modelYear(emptyToNull(req.modelYear))
                    .fuelType(emptyToNull(req.fuelType))
                    .displacement(req.displacement)
                    .firstRegDate(req.firstRegDate)
                    .operationType(emptyToNull(req.operationType))
                    .branch(emptyToNull(req.branch))
                    .inspectionStart(req.inspectionStart)
                    .inspectionEnd(req.inspectionEnd)
                    .vehiclePrice(nvl(req.vehiclePrice))
                    .optionPrice(nvl(req.optionPrice))
                    .orderDate(orderDate)
                    // 차대번호/차량번호는 대별로 나중에 개별 입력. N대 동시 발주 시 공유 방지 위해 1대일 때만 반영.
                    .chassisNo(quantity == 1 ? emptyToNull(req.chassisNo) : null)
                    .releasePrice(req.releasePrice)
                    .totalAdvancePrice(req.totalAdvancePrice)
                    .vehicleNo(quantity == 1 ? emptyToNull(req.vehicleNo) : null)
                    .registerDate(req.registerDate)
                    .registerFileName(emptyToNull(req.registerFileName))
                    .registerFilePath(emptyToNull(req.registerFilePath))
                    .build();

            VehicleOrder saved = orderRepo.save(o);

            // 엑셀 업로드 등 "등록완료" 이후 상태로 직접 생성하면 등록이 끝난 차량이므로
            // 이 자리에서 차량관리번호를 확정한다(발주번호+001, 002, …).
            if (REGISTERED_STATUSES.contains(saved.getOrderStatus())) {
                confirmMgmtNo(saved);
            }

            historyRepo.save(VehicleOrderHistory.builder()
                    .vehicleOrder(saved)
                    .status(saved.getOrderStatus())
                    .changedAt(LocalDateTime.now())
                    .note(quantity > 1 ? ("최초 등록 (" + unit + "/" + quantity + "대)") : "최초 등록")
                    .build());

            // …000은 N행 공유값이라 mgmtNo로 되조회하면 NonUnique → 저장된 엔티티(id)로 응답 구성
            result.add(buildDetailResponse(saved));
        }

        return result;
    }

    // 대별 제조사계약번호 선택: makerContractNos[unit-1]가 있으면 그 값, 없으면 공통 makerContractNo.
    private String pickMakerContractNo(VehicleOrderRequest req, int unit) {
        if (req.makerContractNos != null && req.makerContractNos.size() >= unit) {
            return emptyToNull(req.makerContractNos.get(unit - 1));
        }
        return emptyToNull(req.makerContractNo);
    }

    @Transactional(readOnly = true)
    public VehicleOrderResponse detail(String mgmtNo) {
        // BUG-2 수정: 발주(N대) 시 동일 …000 공유값으로 findByVehicleMgmtNo 가
        // "Query did not return a unique result" 예외를 던지므로 findFirst 로 변경.
        VehicleOrder o = orderRepo.findFirstByVehicleMgmtNoOrderByIdAsc(mgmtNo)
                .orElseThrow(() -> new RuntimeException("차량 없음: " + mgmtNo));
        return buildDetailResponse(o);
    }

    /** 차량등록 화면용 조회: N대 발주 시 "출고완료" 상태인 다음 차량을 순차 반환 */
    @Transactional(readOnly = true)
    public VehicleOrderResponse detailForRegister(String mgmtNo) {
        VehicleOrder o = orderRepo.findFirstByVehicleMgmtNoAndOrderStatusOrderByIdAsc(mgmtNo, "출고완료")
                .orElseThrow(() -> new RuntimeException(
                        "출고완료 상태의 차량이 없습니다. 이미 모두 등록 완료되었거나 출고 전 상태입니다."));
        return buildDetailResponse(o);
    }

    // 발주~선급(pre-실행) 단계 단건 조회는 행 PK(id)로 특정한다(…000 공유 대비).
    @Transactional(readOnly = true)
    public VehicleOrderResponse detailById(Long id) {
        VehicleOrder o = orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("차량 없음(id): " + id));
        return buildDetailResponse(o);
    }

    private VehicleOrderResponse buildDetailResponse(VehicleOrder o) {
        List<VehicleOrderResponse.HistoryItem> history = historyRepo
                .findByVehicleOrder_IdOrderByChangedAtAsc(o.getId())
                .stream()
                .map(h -> VehicleOrderResponse.HistoryItem.builder()
                        .status(h.getStatus())
                        .changedAt(h.getChangedAt())
                        .note(h.getNote())
                        .build())
                .toList();

        VehicleOrderResponse res = toSimple(o);
        res.setHistory(history);
        return res;
    }

    // ✅ 핵심 수정 부분
    // 기존 문제:
    // PUT /api/vehicle-orders/J0010 로 { orderStatus: "실행완료" }만 보내면
    // makerContractNo, optionName, vehiclePrice, optionPrice, chassisNo 등이 null/0으로 덮어써졌음.
    //
    // 수정:
    // req에 들어온 값만 수정하고, 안 들어온 값은 기존 DB 값을 그대로 유지함.
    @Transactional
    public VehicleOrderResponse update(String mgmtNo, VehicleOrderRequest req) {
        // BUG-2 수정: 발주(N대) …000 공유값에서 NonUnique 예외 방지 → findFirst 사용.
        // 실행 후 유니크 번호에서는 findFirst = findBy 와 동일 동작.
        VehicleOrder o = orderRepo.findFirstByVehicleMgmtNoOrderByIdAsc(mgmtNo)
                .orElseThrow(() -> new RuntimeException("차량 없음: " + mgmtNo));
        return applyUpdate(o, req);
    }

    // 발주~선급(pre-실행) 단계 수정은 행 PK(id)로 특정(…000 공유 대비).
    @Transactional
    public VehicleOrderResponse updateById(Long id, VehicleOrderRequest req) {
        VehicleOrder o = orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("차량 없음(id): " + id));
        return applyUpdate(o, req);
    }

    private VehicleOrderResponse applyUpdate(VehicleOrder o, VehicleOrderRequest req) {
        String oldStatus = o.getOrderStatus();
        String oldOperationType = o.getOperationType();
        String oldBranch = o.getBranch();

        if (req.orderStatus != null && !req.orderStatus.isBlank()) {
            o.setOrderStatus(req.orderStatus);
        }

        if (req.carModel != null && !req.carModel.isBlank()) {
            o.setCarModel(req.carModel);
        }

        if (req.makerContractNo != null) {
            o.setMakerContractNo(emptyToNull(req.makerContractNo));
        }

        if (req.optionName != null) {
            o.setOptionName(emptyToNull(req.optionName));
        }

        if (req.modelYear != null) {
            o.setModelYear(emptyToNull(req.modelYear));
        }

        if (req.fuelType != null) {
            o.setFuelType(emptyToNull(req.fuelType));
        }

        if (req.displacement != null) {
            o.setDisplacement(req.displacement);
        }

        if (req.operationType != null) {
            o.setOperationType(emptyToNull(req.operationType));
        }

        if (req.branch != null) {
            o.setBranch(emptyToNull(req.branch));
        }

        if (req.firstRegDate != null) {
            o.setFirstRegDate(req.firstRegDate);
        }

        if (req.inspectionStart != null) {
            o.setInspectionStart(req.inspectionStart);
        }

        if (req.inspectionEnd != null) {
            o.setInspectionEnd(req.inspectionEnd);
        }

        if (req.vehiclePrice != null) {
            o.setVehiclePrice(req.vehiclePrice);
        }

        if (req.optionPrice != null) {
            o.setOptionPrice(req.optionPrice);
        }

        if (req.orderDate != null) {
            o.setOrderDate(req.orderDate);
        }

        if (req.chassisNo != null) {
            o.setChassisNo(emptyToNull(req.chassisNo));
        }

        if (req.releasePrice != null) {
            o.setReleasePrice(req.releasePrice);
        }

        if (req.totalAdvancePrice != null) {
            o.setTotalAdvancePrice(req.totalAdvancePrice);
        }

        if (req.vehicleNo != null) {
            o.setVehicleNo(emptyToNull(req.vehicleNo));
        }

        if (req.registerDate != null) {
            o.setRegisterDate(req.registerDate);
        }

        if (req.registerFileName != null) {
            o.setRegisterFileName(emptyToNull(req.registerFileName));
        }

        if (req.registerFilePath != null) {
            o.setRegisterFilePath(emptyToNull(req.registerFilePath));
        }

        applyAutoStatusStepwise(o);

        orderRepo.save(o);

        // 등록완료(또는 실행완료)가 되는 순간 차량관리번호를 확정한다.
        //   발주단계 VFM260730001000 → 확정 VFM260730001001, 002, …  (이미 확정된 차량은 그대로)
        if (REGISTERED_STATUSES.contains(o.getOrderStatus())) {
            confirmMgmtNo(o);
        }

        if (!Objects.equals(oldStatus, o.getOrderStatus())) {
            historyRepo.save(VehicleOrderHistory.builder()
                    .vehicleOrder(o)
                    .status(o.getOrderStatus())
                    .changedAt(LocalDateTime.now())
                    .note("상태 변경")
                    .build());
        }

        // 운용구분(장기↔단기)·소속지점 변경도 이력으로 남긴다 — 단기 차량이 장기로 나가는 경우 등
        List<String> attrChanges = new ArrayList<>();
        if (!Objects.equals(oldOperationType, o.getOperationType())) {
            attrChanges.add("운용구분 " + orDash(oldOperationType) + " → " + orDash(o.getOperationType()));
        }
        if (!Objects.equals(oldBranch, o.getBranch())) {
            attrChanges.add("소속지점 " + orDash(oldBranch) + " → " + orDash(o.getBranch()));
        }
        if (!attrChanges.isEmpty()) {
            historyRepo.save(VehicleOrderHistory.builder()
                    .vehicleOrder(o)
                    .status(o.getOrderStatus())
                    .changedAt(LocalDateTime.now())
                    .note(String.join(", ", attrChanges))
                    .build());
        }

        return buildDetailResponse(o);
    }

    @Transactional(readOnly = true)
    public List<VehicleStateResponse> vehicleStateList() {
        List<VehicleOrder> orders = orderRepo.findAll();

        // 매각된 차량관리번호 집합
        Set<String> soldMgmtNos = saleRepo.findAll().stream()
                .map(s -> normalize(s.getVehicleMgmtNo()))
                .collect(Collectors.toSet());

        // 종료되지 않은 최신 계약 (차량번호 → 계약)
        Set<String> terminatedStatuses = Set.of(
                "종료", "만기종료", "해지", "중도해지", "중도상환", "만기상환", "완료", "종결"
        );
        List<Contract> allContracts = contractRepo.findAll();
        Map<String, Contract> activeContractByVehicleNo = new LinkedHashMap<>();
        allContracts.forEach(c -> {
            if (c.getVehicleNo() == null) return;
            if (terminatedStatuses.contains(c.getStatus())) return;
            String key = normalize(c.getVehicleNo());
            // 같은 차량에 여러 계약이면 id 큰 것(최신)을 우선
            activeContractByVehicleNo.merge(key, c,
                    (existing, incoming) -> existing.getId() >= incoming.getId() ? existing : incoming);
        });

        List<VehicleStateResponse> result = new ArrayList<>();
        for (VehicleOrder o : orders) {
            String state;
            String contractorName = null;
            LocalDate contractStart = null;
            LocalDate contractEnd = null;

            String mgmtKey = normalize(o.getVehicleMgmtNo());
            String vehicleKey = normalize(o.getVehicleNo());

            if (soldMgmtNos.contains(mgmtKey) || SOLD_STATUS.equals(o.getOrderStatus())) {
                state = "매각";
            } else if (vehicleKey != null && !vehicleKey.isEmpty()
                    && activeContractByVehicleNo.containsKey(vehicleKey)) {
                Contract c = activeContractByVehicleNo.get(vehicleKey);
                state = "계약";
                Customer cust = c.getCustomer();
                contractorName = (cust != null) ? cust.getCustomerName() : null;
                contractStart = c.getStartDate();
                contractEnd = c.getEndDate();
            } else {
                state = "대기";
            }

            result.add(VehicleStateResponse.builder()
                    .vehicleNo(o.getVehicleNo())
                    .vehicleMgmtNo(o.getVehicleMgmtNo())
                    .carModel(o.getCarModel())
                    .vehiclePrice(o.getTotalPrice())
                    .vehicleState(state)
                    .contractorName(contractorName)
                    .contractStartDate(contractStart)
                    .contractEndDate(contractEnd)
                    .registeredDate(o.getRegisterDate() != null ? o.getRegisterDate() : o.getOrderDate())
                    .build());
        }
        return result;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().replace(" ", "").replace("-", "");
    }

    private void applyAutoStatusStepwise(VehicleOrder o) {
        String s = o.getOrderStatus();
        if ("실행완료".equals(s)) return;

        boolean hasContract = o.getMakerContractNo() != null && !o.getMakerContractNo().isBlank();
        boolean hasOrderDate = o.getOrderDate() != null;

        boolean hasReleasePrice = o.getReleasePrice() != null && o.getReleasePrice() > 0;
        boolean hasTotalAdvance = o.getTotalAdvancePrice() != null && o.getTotalAdvancePrice() > 0;

        boolean hasVehicleNo = o.getVehicleNo() != null && !o.getVehicleNo().isBlank();
        boolean hasFirstRegDate = o.getFirstRegDate() != null;
        boolean hasInspStart = o.getInspectionStart() != null;
        boolean hasInspEnd = o.getInspectionEnd() != null;
        boolean hasModelYear = o.getModelYear() != null && !o.getModelYear().isBlank();
        boolean hasFuelType = o.getFuelType() != null && !o.getFuelType().isBlank();
        boolean hasDisplacement = o.getDisplacement() != null && o.getDisplacement() > 0;

        if ("등록완료".equals(s)) return;

        if ("발주전".equals(s) && hasContract && hasOrderDate) {
            o.setOrderStatus("발주완료");
            return;
        }

        if ("발주완료".equals(s) && hasReleasePrice && hasTotalAdvance) {
            o.setOrderStatus("출고완료");
            return;
        }

        if ("출고완료".equals(s)
                && hasVehicleNo
                && hasFirstRegDate
                && hasInspStart
                && hasInspEnd
                && hasModelYear
                && hasFuelType
                && hasDisplacement) {
            o.setOrderStatus("등록완료");
        }
    }

    /**
     * BUG-1/BUG-2 수정: 차입금 스케줄 등록(실행) 엔드포인트 구현
     * - 차량번호를 vehicleMgmtNo로 조회한 뒤 req.vehicleNo 세팅
     * - VehicleLoanService.create() 호출 → 차입금 + 개시전표 자동 생성(BUG-2 해결)
     * - 차량 상태를 "실행완료"로 변경
     */
    @Transactional
    public VehicleDeliveryExecuteResponse execute(String vehicleMgmtNo, VehicleLoanCreateRequest req) {
        // 발주단계(…000)를 5대가 공유할 수 있으므로 단건 조회로는 특정할 수 없다.
        List<VehicleOrder> candidates = orderRepo.findByVehicleMgmtNoOrderByIdAsc(vehicleMgmtNo);
        if (candidates.isEmpty()) {
            throw new RuntimeException("차량 없음: " + vehicleMgmtNo);
        }
        if (candidates.size() > 1) {
            throw new RuntimeException(
                    "같은 차량관리번호를 가진 차량이 " + candidates.size() + "대입니다. "
                            + "실행할 차량을 선택해 주세요. (id 기준 실행 사용)");
        }
        return executeOrder(candidates.get(0), req);
    }

    /** 행 PK(id) 기준 실행 — N대 발주에서 실행할 차량을 지정할 때 사용 */
    @Transactional
    public VehicleDeliveryExecuteResponse executeById(Long id, VehicleLoanCreateRequest req) {
        VehicleOrder o = orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("차량 없음(id): " + id));
        return executeOrder(o, req);
    }

    private VehicleDeliveryExecuteResponse executeOrder(VehicleOrder o, VehicleLoanCreateRequest req) {
        String vehicleNo = o.getVehicleNo();
        if (vehicleNo == null || vehicleNo.isBlank()) {
            throw new RuntimeException("차량번호가 등록되지 않은 차량입니다. 차량등록 완료 후 실행해주세요.");
        }

        // 차입금·개시전표가 확정번호로 만들어지도록 번호부터 확정한다(등록 단계에서 이미 확정됐으면 그대로).
        confirmMgmtNo(o);

        // 차입금 생성 (내부에서 개시전표도 자동 생성됨)
        req.vehicleNo = vehicleNo;
        VehicleLoanListItemResponse loan = vehicleLoanService.create(req);

        // 상태가 아직 실행완료가 아니면 변경
        if (!"실행완료".equals(o.getOrderStatus())) {
            o.setOrderStatus("실행완료");
            orderRepo.save(o);
            historyRepo.save(VehicleOrderHistory.builder()
                    .vehicleOrder(o)
                    .status("실행완료")
                    .changedAt(LocalDateTime.now())
                    .note("차입금 스케줄 등록 실행 (관리번호 " + o.getVehicleMgmtNo() + ")")
                    .build());
        }

        return VehicleDeliveryExecuteResponse.builder()
                .vehicleMgmtNo(o.getVehicleMgmtNo())
                .orderStatus(o.getOrderStatus())
                .loanId(loan.getId())
                .build();
    }

    @Transactional
    public void registerVehicle(String mgmtNo, VehicleRegisterRequest req, MultipartFile file) {
        // N대 발주 시 동일 mgmtNo(…000) 공유 → 등록 순서대로 "출고완료" 상태인 행을 순차 처리.
        // findFirst(id ASC)는 이미 "등록완료"된 첫 행을 반환해 2번째부터 오류 발생 → 상태 필터 추가.
        VehicleOrder o = orderRepo.findFirstByVehicleMgmtNoAndOrderStatusOrderByIdAsc(mgmtNo, "출고완료")
                .orElseThrow(() -> new RuntimeException("출고완료 상태의 차량이 없습니다. 이미 모두 등록 완료되었거나 출고 전 상태입니다."));


        if (req.getVehicleNo() == null || req.getVehicleNo().isBlank()) {
            throw new RuntimeException("차량번호(vehicleNo)는 필수");
        }
        // BUG-07 수정: firstRegDate(최초등록일) 필수 — registerDate 는 optional(시스템 등록일)
        if (req.getFirstRegDate() == null) {
            throw new RuntimeException("최초등록일(firstRegDate)은 필수");
        }

        String oldStatus = o.getOrderStatus();

        o.setVehicleNo(req.getVehicleNo());
        // BUG-07: firstRegDate(자동차등록증 상 최초등록일)를 엔티티에 저장
        o.setFirstRegDate(req.getFirstRegDate());
        // registerDate 는 시스템 등록완료일 — 미제공 시 오늘
        o.setRegisterDate(req.getRegisterDate() != null ? req.getRegisterDate() : LocalDate.now());
        if (req.getInspectionStart() != null) o.setInspectionStart(req.getInspectionStart());
        if (req.getInspectionEnd() != null)   o.setInspectionEnd(req.getInspectionEnd());

        if (file != null && !file.isEmpty()) {
            String savedPath = saveFile(mgmtNo, file);
            o.setRegisterFileName(file.getOriginalFilename());
            o.setRegisterFilePath(savedPath);
        }

        o.setOrderStatus("등록완료");

        orderRepo.save(o);

        // 등록이 끝났으므로 차량관리번호를 확정한다.
        confirmMgmtNo(o);

        if (!Objects.equals(oldStatus, "등록완료")) {
            historyRepo.save(VehicleOrderHistory.builder()
                    .vehicleOrder(o)
                    .status("등록완료")
                    .changedAt(LocalDateTime.now())
                    .note("차량 등록 완료 (관리번호 " + o.getVehicleMgmtNo() + ")")
                    .build());
        }
    }

    @Transactional
    public VehicleOrderResponse registerVehicleById(Long id, VehicleRegisterRequest req, MultipartFile file) {
        VehicleOrder o = orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("차량 없음(id): " + id));

        if (!"출고완료".equals(o.getOrderStatus())) {
            throw new RuntimeException("출고완료 상태의 차량만 등록할 수 있습니다.");
        }
        if (req.getVehicleNo() == null || req.getVehicleNo().isBlank()) {
            throw new RuntimeException("차량번호(vehicleNo)는 필수");
        }
        // BUG-07 수정: firstRegDate(최초등록일) 필수 체크
        if (req.getFirstRegDate() == null) {
            throw new RuntimeException("최초등록일(firstRegDate)은 필수");
        }

        String oldStatus = o.getOrderStatus();

        o.setVehicleNo(req.getVehicleNo());
        // BUG-07: firstRegDate(자동차등록증 상 최초등록일) 저장
        o.setFirstRegDate(req.getFirstRegDate());
        o.setRegisterDate(req.getRegisterDate() != null ? req.getRegisterDate() : LocalDate.now());
        if (req.getInspectionStart() != null) o.setInspectionStart(req.getInspectionStart());
        if (req.getInspectionEnd() != null)   o.setInspectionEnd(req.getInspectionEnd());
        if (req.getModelYear() != null)        o.setModelYear(req.getModelYear());
        if (req.getFuelType() != null)         o.setFuelType(req.getFuelType());
        if (req.getDisplacement() != null)     o.setDisplacement(req.getDisplacement());

        if (file != null && !file.isEmpty()) {
            String savedPath = saveFile(o.getVehicleMgmtNo(), file);
            o.setRegisterFileName(file.getOriginalFilename());
            o.setRegisterFilePath(savedPath);
        }

        o.setOrderStatus("등록완료");

        orderRepo.save(o);

        // 등록이 끝났으므로 차량관리번호를 확정한다.
        confirmMgmtNo(o);

        if (!Objects.equals(oldStatus, "등록완료")) {
            historyRepo.save(VehicleOrderHistory.builder()
                    .vehicleOrder(o)
                    .status("등록완료")
                    .changedAt(LocalDateTime.now())
                    .note("차량 등록 완료 (관리번호 " + o.getVehicleMgmtNo() + ")")
                    .build());
        }

        return buildDetailResponse(o);
    }

    /**
     * 차량관리번호 실번호 확정 (등록완료 시점).
     *
     * <p>차량관리번호(15자리) = 발주번호(12) + 대순번(3)
     * <p>대순번은 같은 발주 안에서 확정되는 순서대로 001부터.
     * <p>예) 발주 VFM260804001 을 5대 실행 → VFM260804001001 ~ VFM260804001005
     */
    private void confirmVehicleMgmtNo(VehicleOrder o) {
        String orderNo = o.getOrderNo();
        if (orderNo == null || orderNo.length() != 12) {
            throw new RuntimeException(
                    "발주번호가 없어 차량관리번호를 확정할 수 없습니다. (id=" + o.getId() + ")");
        }
        int unitSeq = numberGenerator.nextUnitSeq(orderNo);
        o.setVehicleMgmtNo(VehicleNumberGenerator.buildVehicleMgmtNo(orderNo, unitSeq));
    }

    /**
     * 차량관리번호로 실행 대상 차량 목록을 조회한다.
     *
     * <p>발주단계 번호(…000)를 5대가 공유하므로, 실행 화면에서는 이 목록을 띄우고
     * 사용자가 실행할 차량을 고른다. 1대뿐이면 목록도 1건이다.
     */
    @Transactional(readOnly = true)
    public List<VehicleOrderResponse> findExecutableByMgmtNo(String mgmtNo) {
        return orderRepo.findByVehicleMgmtNoOrderByIdAsc(mgmtNo).stream()
                .map(this::buildDetailResponse)
                .toList();
    }

    /**
     * 재렌트 시 차량관리번호는 변경하지 않는다. (v5 개정: 회차 증가 로직 제거)
     * 실행 확정된 차량인지(15자리, "000" 아님) 확인 후 현재 번호를 그대로 반환한다.
     *
     * @return 현재 차량관리번호 (변경 없음)
     */
    @Transactional
    public String applyRerent(String vehicleNo) {
        VehicleOrder o = orderRepo.findByVehicleNoNormalized(vehicleNo)
                .orElseThrow(() -> new RuntimeException("차량 없음(차량번호): " + vehicleNo));

        String mgmtNo = o.getVehicleMgmtNo();
        if (mgmtNo == null || mgmtNo.length() != 15 || mgmtNo.endsWith("000")) {
            throw new RuntimeException(
                    "실행 확정된 차량만 재렌트할 수 있습니다. 현재 차량관리번호=" + mgmtNo);
        }

        // 재렌트 시 차량관리번호는 변경하지 않으므로 이력만 기록한다.
        historyRepo.save(VehicleOrderHistory.builder()
                .vehicleOrder(o)
                .status(o.getOrderStatus())
                .changedAt(LocalDateTime.now())
                .note("재렌트 — 차량관리번호 유지: " + mgmtNo)
                .build());

        return mgmtNo;
    }

    /** 발주단계(…000) 임시번호인지 */
    private boolean isOrderStageMgmtNo(VehicleOrder o) {
        return o.getVehicleMgmtNo() != null
                && o.getVehicleMgmtNo().endsWith(VehicleNumberGenerator.ORDER_STAGE_SUFFIX);
    }

    /**
     * 차량관리번호를 확정하고, 임시번호(…000)로 붙어 있던 기록의 번호도 함께 갱신한다.
     * 등록이 끝나는 시점(등록완료)에 부르며, 이미 확정된 차량이면 아무것도 하지 않는다.
     *
     * <p>N대 발주는 …000 을 함께 쓰므로, 차량 행 id 를 가진 테이블은 id 로 이 차량 것만 바꾸고,
     * id 가 없는 테이블은 이 차량이 임시번호의 마지막 보유자일 때만 바꾼다.
     * 전표는 기존 규칙(임시번호 전표 전체를 처음 확정되는 차량으로 이동)을 유지한다.
     */
    private void confirmMgmtNo(VehicleOrder o) {
        if (!isOrderStageMgmtNo(o)) return;
        String oldMgmtNo = o.getVehicleMgmtNo();
        confirmVehicleMgmtNo(o);
        orderRepo.save(o);
        String newMgmtNo = o.getVehicleMgmtNo();
        if (Objects.equals(oldMgmtNo, newMgmtNo)) return;

        orderRepo.flush();
        for (String table : MGMT_NO_TABLES_WITH_ORDER_ID) {
            renameMgmtNo(table, "WHERE vehicle_mgmt_no = ? AND vehicle_order_id = ?", newMgmtNo, oldMgmtNo, o.getId());
        }
        boolean stillShared = !orderRepo.findByVehicleMgmtNoOrderByIdAsc(oldMgmtNo).isEmpty();
        if (!stillShared) {
            for (String table : MGMT_NO_TABLES_WITHOUT_ORDER_ID) {
                renameMgmtNo(table, "WHERE vehicle_mgmt_no = ?", newMgmtNo, oldMgmtNo);
            }
        }
        voucherService.updateVehicleMgmtNo(oldMgmtNo, newMgmtNo);
    }

    /** 테이블이 아직 없는 회사 DB(해당 기능 미사용)는 바꿀 행도 없으므로 건너뛴다. */
    private void renameMgmtNo(String table, String where, Object... args) {
        try {
            jdbcTemplate.update("UPDATE " + table + " SET vehicle_mgmt_no = ? " + where, args);
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            log.warn("[차량관리번호 확정] {} 갱신 생략(테이블 없음?): {}", table, e.getMostSpecificCause().getMessage());
        }
    }

    /**
     * 등록이 끝났는데 아직 임시번호(…000)인 차량을 모두 확정번호로 바꾼다.
     * 등록 시점 확정 규칙 이전에 등록된 차량을 정리하는 1회성 작업. 여러 번 실행해도 안전하다.
     *
     * @return 확정된 차량별 {id, vehicleNo, oldMgmtNo, newMgmtNo}
     */
    @Transactional
    public List<Map<String, Object>> confirmRegisteredMgmtNos() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<VehicleOrder> targets = orderRepo.findAll().stream()
                .filter(o -> REGISTERED_STATUSES.contains(o.getOrderStatus()))
                .filter(this::isOrderStageMgmtNo)
                .sorted(Comparator.comparing(VehicleOrder::getId))
                .toList();
        for (VehicleOrder o : targets) {
            String oldMgmtNo = o.getVehicleMgmtNo();
            confirmMgmtNo(o);
            historyRepo.save(VehicleOrderHistory.builder()
                    .vehicleOrder(o)
                    .status(o.getOrderStatus())
                    .changedAt(LocalDateTime.now())
                    .note("차량관리번호 확정 " + oldMgmtNo + " → " + o.getVehicleMgmtNo())
                    .build());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", o.getId());
            row.put("vehicleNo", o.getVehicleNo());
            row.put("oldMgmtNo", oldMgmtNo);
            row.put("newMgmtNo", o.getVehicleMgmtNo());
            result.add(row);
        }
        return result;
    }

    private String saveFile(String mgmtNo, MultipartFile file) {
        try {
            String original = (file.getOriginalFilename() == null) ? "file" : file.getOriginalFilename();
            String safe = original.replaceAll("[\\\\/:*?\"<>|]", "_");
            String key = "vehicle-register/" + mgmtNo + "_" + UUID.randomUUID() + "_" + safe;

            storage.put(key, file.getBytes(), file.getContentType());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("파일 저장 실패: " + e.getMessage());
        }
    }

    private VehicleOrderResponse toSimple(VehicleOrder o) {
        return VehicleOrderResponse.builder()
                .id(o.getId())
                .vehicleMgmtNo(o.getVehicleMgmtNo())
                .orderNo(o.getOrderNo())
                .orderStatus(o.getOrderStatus())
                .makerContractNo(o.getMakerContractNo())
                .carModel(o.getCarModel())
                .optionName(o.getOptionName())
                .modelYear(o.getModelYear())
                .fuelType(o.getFuelType())
                .displacement(o.getDisplacement())
                .firstRegDate(o.getFirstRegDate())
                .operationType(o.getOperationType())
                .branch(o.getBranch())
                .inspectionStart(o.getInspectionStart())
                .inspectionEnd(o.getInspectionEnd())
                .vehiclePrice(o.getVehiclePrice())
                .optionPrice(o.getOptionPrice())
                .totalPrice(o.getTotalPrice())
                .orderDate(o.getOrderDate())
                .chassisNo(o.getChassisNo())
                .releasePrice(o.getReleasePrice())
                .totalAdvancePrice(o.getTotalAdvancePrice())
                .vehicleNo(o.getVehicleNo())
                .registerDate(o.getRegisterDate())
                .registerFileName(o.getRegisterFileName())
                .registerFilePath(o.getRegisterFilePath())
                .build();
    }

    private long nvl(Long v) {
        return v == null ? 0L : v;
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String orDash(String s) {
        return (s == null || s.isBlank()) ? "미지정" : s;
    }
}