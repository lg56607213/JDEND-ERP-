package com.jdend.erp.vehicle.service;

import com.jdend.erp.accounting.settings.service.OtherAccountSettingsService;
import com.jdend.erp.accounting.voucher.dto.VoucherCreateRequest;
import com.jdend.erp.accounting.voucher.service.VoucherService;
import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.customer.Customer;
import com.jdend.erp.vehicle.dto.*;
import com.jdend.erp.vehicle.entity.*;
import com.jdend.erp.vehicle.repository.*;
import com.jdend.erp.vehicle.sale.repository.VehicleSaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleOrderService {

    private final VehicleOrderRepository orderRepo;
    private final VehicleOrderHistoryRepository historyRepo;
    private final VehicleLoanRepository loanRepo;
    private final VoucherService voucherService;
    private final OtherAccountSettingsService accountSettings;
    private final VehicleSaleRepository saleRepo;
    private final ContractRepository contractRepo;
    private final VehicleNumberGenerator numberGenerator;

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
        List<VehicleOrder> list;

        boolean hasDate = (start != null && end != null);
        boolean hasStatus = (status != null && !status.isBlank());

        if (hasDate && hasStatus) {
            list = orderRepo.findByOrderStatusAndOrderDateBetween(status, start, end);
        } else if (hasDate) {
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
                    .orderStatus("발주전")
                    .makerContractNo(pickMakerContractNo(req, unit))
                    .carModel(req.carModel)
                    .optionName(emptyToNull(req.optionName))
                    .modelYear(emptyToNull(req.modelYear))
                    .fuelType(emptyToNull(req.fuelType))
                    .displacement(req.displacement)
                    .firstRegDate(req.firstRegDate)
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
        // 주의: 발주(미실행) …000은 공유값이라 여러 행이면 NonUnique. 실행 후 유니크 번호에만 안전.
        VehicleOrder o = orderRepo.findByVehicleMgmtNo(mgmtNo)
                .orElseThrow(() -> new RuntimeException("차량 없음: " + mgmtNo));
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
        // 주의: 발주(미실행) …000 공유값에는 부적합(NonUnique). 실행 후 유니크 번호에만 사용.
        VehicleOrder o = orderRepo.findByVehicleMgmtNo(mgmtNo)
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

        // S3: 등록완료 전환 시 …000 발주번호 → 실번호 확정
        if ("등록완료".equals(o.getOrderStatus())
                && o.getVehicleMgmtNo() != null
                && o.getVehicleMgmtNo().endsWith(VehicleNumberGenerator.ORDER_STAGE_SUFFIX)) {
            confirmVehicleMgmtNo(o);
        }

        orderRepo.save(o);

        if (!Objects.equals(oldStatus, o.getOrderStatus())) {
            historyRepo.save(VehicleOrderHistory.builder()
                    .vehicleOrder(o)
                    .status(o.getOrderStatus())
                    .changedAt(LocalDateTime.now())
                    .note("상태 변경")
                    .build());
        }

        return buildDetailResponse(o);
    }

    @Transactional
    public VehicleDeliveryExecuteResponse executeDelivery(String mgmtNo, VehicleDeliveryExecuteRequest req) {

        VehicleOrder o = orderRepo.findByVehicleMgmtNo(mgmtNo)
                .orElseThrow(() -> new RuntimeException("차량 없음: " + mgmtNo));

        if (!"실행완료".equals(o.getOrderStatus())) {
            throw new RuntimeException("실행완료 상태만 차입금 스케줄 등록이 가능합니다. 현재상태=" + o.getOrderStatus());
        }

        if (o.getVehicleNo() == null || o.getVehicleNo().isBlank()) {
            throw new RuntimeException("차량번호가 없는 차량은 차입금 스케줄 등록이 불가능합니다.");
        }

        List<VehicleLoan> existingLoans = loanRepo.findByVehicleNoNormalizedOrderByIdDesc(o.getVehicleNo());
        for (VehicleLoan existing : existingLoans) {
            if (existing.getTerminated() == null || !existing.getTerminated()) {
                throw new RuntimeException("이미 등록된 차입금 스케줄이 있습니다. 기존 차입금현황에서 확인해주세요.");
            }
        }

        if (req.loanPrincipal == null || req.loanPrincipal <= 0) {
            throw new RuntimeException("대출원금 필수");
        }
        if (req.loanInterest == null) {
            throw new RuntimeException("대출이자 필수");
        }
        if (req.financeName == null || req.financeName.isBlank()) {
            throw new RuntimeException("금융사명 필수");
        }
        if (req.repaymentMethod == null || req.repaymentMethod.isBlank()) {
            throw new RuntimeException("상환방식 필수");
        }
        if (req.repaymentPeriod == null || req.repaymentPeriod <= 0) {
            throw new RuntimeException("상환기간 필수");
        }
        if (req.startDate == null) {
            throw new RuntimeException("시작일 필수");
        }
        if (req.paymentDay == null || req.paymentDay < 1 || req.paymentDay > 31) {
            throw new RuntimeException("매월 상환일자(1~31) 필수");
        }

        VehicleLoan savedLoan = loanRepo.save(
                VehicleLoan.builder()
                        .vehicleOrder(o)
                        .vehicleMgmtNo(o.getVehicleMgmtNo())
                        .loanType((req.loanType == null || req.loanType.isBlank()) ? "금융리스" : req.loanType)
                        .loanPrincipal(req.loanPrincipal)
                        .loanInterest(req.loanInterest)
                        .financeName(req.financeName)
                        .repaymentMethod(req.repaymentMethod)
                        .repaymentPeriod(req.repaymentPeriod)
                        .downPayment(req.downPayment == null ? 0L : req.downPayment)
                        .startDate(req.startDate)
                        .endDate(req.endDate)
                        .deposit(req.deposit == null ? 0L : req.deposit)
                        .paymentDay(req.paymentDay)
                        .monthlyPayment(req.monthlyPayment)
                        .repaymentAccount(req.repaymentAccount)
                        .remainingPrincipal(req.loanPrincipal)
                        .lastPaymentDate(null)
                        .terminated(false)
                        .terminatedAt(null)
                        .build()
        );

        createLoanOpenVoucher(savedLoan);
        createVehicleExecuteVoucher(o, req);

        return VehicleDeliveryExecuteResponse.builder()
                .vehicleMgmtNo(o.getVehicleMgmtNo())
                .orderStatus(o.getOrderStatus())
                .loanId(savedLoan.getId())
                .build();
    }

    private void createLoanOpenVoucher(VehicleLoan loan) {
        VehicleOrder order = loan.getVehicleOrder();
        String vehicleNo = order != null ? order.getVehicleNo() : "";
        String contractNo = order != null ? order.getMakerContractNo() : null;

        String memo = (vehicleNo == null || vehicleNo.isBlank())
                ? "차입금 등록"
                : vehicleNo + " 차입금 등록";

        voucherService.create(
                VoucherCreateRequest.builder()
                        .voucherDate(loan.getStartDate() != null ? loan.getStartDate() : LocalDate.now())
                        .contractNumber(contractNo)
                        .vehicleNo(vehicleNo)
                        .vehicleMgmtNo(order != null ? order.getVehicleMgmtNo() : null)
                        .memo(memo)
                        .debitEntries(List.of(
                                VoucherCreateRequest.VoucherLineRequest.builder()
                                        .account("미수금")
                                        .amount(loan.getLoanPrincipal())
                                        .description("대출원금")
                                        .build()
                        ))
                        .creditEntries(List.of(
                                VoucherCreateRequest.VoucherLineRequest.builder()
                                        .account("장기차입금")
                                        .amount(loan.getLoanPrincipal())
                                        .description("대출원금")
                                        .build()
                        ))
                        .build()
        );
    }

    private void createVehicleExecuteVoucher(VehicleOrder order, VehicleDeliveryExecuteRequest req) {
        String debitAccount = accountSettings.getVehicleDebitAccount();
        if (debitAccount == null) debitAccount = "차량운반구";
        String creditAccount = accountSettings.getVehicleCreditAccount();
        if (creditAccount == null) creditAccount = "선급렌트자산";

        long totalAdvance = order.getTotalAdvancePrice() != null ? order.getTotalAdvancePrice() : 0L;
        if (totalAdvance <= 0) return;

        String vehicleNo = order.getVehicleNo();
        String contractNo = order.getMakerContractNo();
        String memo = (vehicleNo == null || vehicleNo.isBlank() ? order.getVehicleMgmtNo() : vehicleNo) + " 차량 실행";
        LocalDate voucherDate = req.startDate != null ? req.startDate : LocalDate.now();

        voucherService.create(
                VoucherCreateRequest.builder()
                        .voucherDate(voucherDate)
                        .contractNumber(contractNo)
                        .vehicleNo(vehicleNo)
                        .vehicleMgmtNo(order.getVehicleMgmtNo())
                        .memo(memo)
                        .debitEntries(List.of(
                                VoucherCreateRequest.VoucherLineRequest.builder()
                                        .account(debitAccount)
                                        .amount(totalAdvance)
                                        .description("선급렌트자산 → " + debitAccount)
                                        .build()
                        ))
                        .creditEntries(List.of(
                                VoucherCreateRequest.VoucherLineRequest.builder()
                                        .account(creditAccount)
                                        .amount(totalAdvance)
                                        .description("선급렌트자산 대체")
                                        .build()
                        ))
                        .build()
        );
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

            if (soldMgmtNos.contains(mgmtKey)) {
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

    @Transactional
    public void registerVehicle(String mgmtNo, VehicleRegisterRequest req, MultipartFile file) {
        VehicleOrder o = orderRepo.findByVehicleMgmtNo(mgmtNo)
                .orElseThrow(() -> new RuntimeException("차량 없음: " + mgmtNo));

        if (!"출고완료".equals(o.getOrderStatus())) {
            throw new RuntimeException("출고완료 상태의 차량만 등록할 수 있습니다.");
        }

        if (req.getVehicleNo() == null || req.getVehicleNo().isBlank()) {
            throw new RuntimeException("차량번호(vehicleNo)는 필수");
        }
        if (req.getRegisterDate() == null) {
            throw new RuntimeException("등록일자(registerDate)은 필수");
        }

        String oldStatus = o.getOrderStatus();

        o.setVehicleNo(req.getVehicleNo());
        o.setRegisterDate(req.getRegisterDate());

        if (file != null && !file.isEmpty()) {
            String savedPath = saveFile(mgmtNo, file);
            o.setRegisterFileName(file.getOriginalFilename());
            o.setRegisterFilePath(savedPath);
        }

        o.setOrderStatus("등록완료");
        orderRepo.save(o);

        if (!Objects.equals(oldStatus, "등록완료")) {
            historyRepo.save(VehicleOrderHistory.builder()
                    .vehicleOrder(o)
                    .status("등록완료")
                    .changedAt(LocalDateTime.now())
                    .note("차량 등록 완료")
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
        if (req.getRegisterDate() == null) {
            throw new RuntimeException("등록일자(registerDate)은 필수");
        }

        String oldStatus = o.getOrderStatus();
        o.setVehicleNo(req.getVehicleNo());
        o.setRegisterDate(req.getRegisterDate());

        if (file != null && !file.isEmpty()) {
            String savedPath = saveFile(o.getVehicleMgmtNo(), file);
            o.setRegisterFileName(file.getOriginalFilename());
            o.setRegisterFilePath(savedPath);
        }

        o.setOrderStatus("등록완료");

        // S3: …000 → 실번호 확정
        if (o.getVehicleMgmtNo() != null
                && o.getVehicleMgmtNo().endsWith(VehicleNumberGenerator.ORDER_STAGE_SUFFIX)) {
            confirmVehicleMgmtNo(o);
        }

        orderRepo.save(o);

        if (!Objects.equals(oldStatus, "등록완료")) {
            historyRepo.save(VehicleOrderHistory.builder()
                    .vehicleOrder(o)
                    .status("등록완료")
                    .changedAt(LocalDateTime.now())
                    .note("차량 등록 완료")
                    .build());
        }

        return buildDetailResponse(o);
    }

    private void confirmVehicleMgmtNo(VehicleOrder o) {
        String orderNo = o.getVehicleMgmtNo().substring(0, 10); // 발주번호 10자리
        // 같은 발주 내 이미 확정된(실번호, 000이 아닌) 대 수 = 다음 대순번 계산
        long confirmedCount = orderRepo.findByOrderNoOrderByVehicleMgmtNoAsc(orderNo)
                .stream()
                .filter(v -> !v.getVehicleMgmtNo().endsWith(VehicleNumberGenerator.ORDER_STAGE_SUFFIX)
                        && !v.getId().equals(o.getId()))
                .count();
        int unitSeq = (int) confirmedCount + 1; // 1-based
        String realMgmtNo = VehicleNumberGenerator.buildVehicleMgmtNo(orderNo, 0, unitSeq);
        o.setVehicleMgmtNo(realMgmtNo);
    }

    private String saveFile(String mgmtNo, MultipartFile file) {
        try {
            Path baseDir = Paths.get("uploads", "vehicle-register");
            Files.createDirectories(baseDir);

            String original = (file.getOriginalFilename() == null) ? "file" : file.getOriginalFilename();
            String safe = original.replaceAll("[\\\\/:*?\"<>|]", "_");
            String filename = mgmtNo + "_" + UUID.randomUUID() + "_" + safe;

            Path target = baseDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
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
}