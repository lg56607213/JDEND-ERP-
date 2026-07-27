package com.jdend.erp.accounting.deposit.service;

import com.jdend.erp.accounting.deposit.dto.DepositCreateRequest;
import com.jdend.erp.accounting.deposit.dto.DepositHistoryResponse;
import com.jdend.erp.accounting.deposit.dto.DepositListItemResponse;
import com.jdend.erp.accounting.deposit.entity.DepositTransaction;
import com.jdend.erp.accounting.deposit.repository.DepositTransactionRepository;
import com.jdend.erp.accounting.settings.service.OtherAccountSettingsService;
import com.jdend.erp.accounting.voucher.dto.VoucherCreateRequest;
import com.jdend.erp.accounting.voucher.service.VoucherService;
import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DepositTransactionService {

    private final DepositTransactionRepository depositRepo;
    private final ContractRepository contractRepo;
    private final VoucherService voucherService;
    private final OtherAccountSettingsService settingsService;

    // ─────────────────────────────────────────────────────────────────────
    // 목록 조회
    // ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DepositListItemResponse> list(LocalDate startDate, LocalDate endDate,
                                               String type, String customerName) {

        String custNameParam = (customerName == null) ? "" : customerName.trim();
        List<Contract> contracts = contractRepo.findDepositContracts(custNameParam, startDate, endDate);
        if (contracts.isEmpty()) return Collections.emptyList();

        // 계약 ID 목록으로 집계 쿼리 2개만 실행
        List<Long> contractIds = contracts.stream().map(Contract::getId).toList();

        // sumByContractIds: [contractId, depositType, transactionType, sum]
        Map<String, Long> sumMap = new HashMap<>();
        for (Object[] row : depositRepo.sumByContractIds(contractIds)) {
            Long cid = toLong(row[0]);
            String dt  = (String) row[1];
            String tt  = (String) row[2];
            Long   amt = toLong(row[3]);
            sumMap.put(key(cid, dt, tt), amt);
        }

        // lastDateByContractIds: [contractId, depositType, maxDate]
        Map<String, LocalDate> lastMap = new HashMap<>();
        for (Object[] row : depositRepo.lastDateByContractIds(contractIds)) {
            Long cid = toLong(row[0]);
            String dt = (String) row[1];
            LocalDate d = (LocalDate) row[2];
            lastMap.put(key2(cid, dt), d);
        }

        List<DepositListItemResponse> result = new ArrayList<>();

        for (Contract c : contracts) {
            String custName = c.getCustomer() != null ? c.getCustomer().getCustomerName() : "";

            // 보증금 행
            boolean depositApplicable = (type == null || type.isBlank()
                    || "전체".equals(type) || "보증금".equals(type));
            if (depositApplicable && c.getDeposit() != null && c.getDeposit() > 0) {
                result.add(buildItem(c, "보증금", c.getDeposit(), custName, sumMap, lastMap));
            }

            // 선수금 행
            boolean advanceApplicable = (type == null || type.isBlank()
                    || "전체".equals(type) || "선수금".equals(type));
            if (advanceApplicable && c.getAdvancePayment() != null && c.getAdvancePayment() > 0) {
                result.add(buildItem(c, "선수금", c.getAdvancePayment(), custName, sumMap, lastMap));
            }
        }

        return result;
    }

    private DepositListItemResponse buildItem(Contract c, String depositType, Long contractAmt,
                                               String custName,
                                               Map<String, Long> sumMap,
                                               Map<String, LocalDate> lastMap) {
        long collected = Optional.ofNullable(sumMap.get(key(c.getId(), depositType, "수납"))).orElse(0L);
        long refunded  = Optional.ofNullable(sumMap.get(key(c.getId(), depositType, "지급"))).orElse(0L);

        return DepositListItemResponse.builder()
                .contractId(c.getId())
                .contractNumber(c.getContractNumber())
                .customerName(custName)
                .vehicleNo(c.getVehicleNo())
                .depositType(depositType)
                .contractDepositAmount(contractAmt)
                .totalCollected(collected)
                .totalRefunded(refunded)
                .balance(collected - refunded)
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .lastTransactionDate(lastMap.get(key2(c.getId(), depositType)))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // 거래 이력 조회
    // ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DepositHistoryResponse> history(Long contractId, String depositType) {
        String dt = (depositType == null || depositType.isBlank()) ? "보증금" : depositType;
        List<DepositTransaction> txList =
                depositRepo.findByContractIdAndDepositTypeOrderByTransactionDateAscIdAsc(contractId, dt);

        List<DepositHistoryResponse> result = new ArrayList<>();
        long cumulative = 0L;

        for (DepositTransaction tx : txList) {
            if ("수납".equals(tx.getTransactionType())) {
                cumulative += tx.getAmount();
            } else {
                cumulative -= tx.getAmount();
            }
            result.add(DepositHistoryResponse.builder()
                    .id(tx.getId())
                    .transactionDate(tx.getTransactionDate())
                    .transactionType(tx.getTransactionType())
                    .amount(tx.getAmount())
                    .cumulativeBalance(cumulative)
                    .memo(tx.getMemo())
                    .voucherCreated(tx.getVoucherCreated())
                    .build());
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────
    // 수납 등록
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public void collect(DepositCreateRequest req) {
        validateRequest(req);
        String depositType = resolveDepositType(req);

        DepositTransaction tx = DepositTransaction.builder()
                .contractId(req.getContractId())
                .depositType(depositType)
                .transactionType("수납")
                .amount(req.getAmount())
                .transactionDate(req.getTransactionDate())
                .memo(req.getMemo())
                .voucherCreated(Boolean.TRUE.equals(req.getCreateVoucher()))
                .build();
        depositRepo.save(tx);

        if (Boolean.TRUE.equals(req.getCreateVoucher())) {
            createCollectVoucher(req, depositType);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 지급(반환) 등록
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public void refund(DepositCreateRequest req) {
        validateRequest(req);
        String depositType = resolveDepositType(req);

        // 잔액 초과 지급 방지
        List<Long> ids = List.of(req.getContractId());
        Map<String, Long> sumMap = new HashMap<>();
        for (Object[] row : depositRepo.sumByContractIds(ids)) {
            sumMap.put(key(toLong(row[0]), (String) row[1], (String) row[2]), toLong(row[3]));
        }
        long collected = Optional.ofNullable(sumMap.get(key(req.getContractId(), depositType, "수납"))).orElse(0L);
        long refunded  = Optional.ofNullable(sumMap.get(key(req.getContractId(), depositType, "지급"))).orElse(0L);
        long balance   = collected - refunded;

        if (req.getAmount() > balance) {
            throw new IllegalArgumentException(
                    "지급 금액(" + fmt(req.getAmount()) + "원)이 " + depositType +
                    " 잔액(" + fmt(balance) + "원)을 초과합니다.");
        }

        DepositTransaction tx = DepositTransaction.builder()
                .contractId(req.getContractId())
                .depositType(depositType)
                .transactionType("지급")
                .amount(req.getAmount())
                .transactionDate(req.getTransactionDate())
                .memo(req.getMemo())
                .voucherCreated(Boolean.TRUE.equals(req.getCreateVoucher()))
                .build();
        depositRepo.save(tx);

        if (Boolean.TRUE.equals(req.getCreateVoucher())) {
            createRefundVoucher(req, depositType);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 전표 생성 헬퍼
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 수납 전표
     *   차변: 보통예금 (depositCreditAccount 또는 "보통예금")
     *   대변: 임대보증금 (depositDebitAccount  또는 "임대보증금")
     */
    private void createCollectVoucher(DepositCreateRequest req, String depositType) {
        String debitAcct  = nvl(settingsService.getDepositCreditAccount(), "보통예금");   // 차변
        String creditAcct = nvl(settingsService.getDepositDebitAccount(),  "임대보증금"); // 대변

        Contract contract = findContract(req.getContractId());
        String memoText = depositType + " 수납" + appendMemo(req.getMemo());

        voucherService.create(VoucherCreateRequest.builder()
                .voucherDate(req.getTransactionDate())
                .contractNumber(contract.getContractNumber())
                .vehicleNo(contract.getVehicleNo())
                .memo(memoText)
                .debitEntries(List.of(VoucherCreateRequest.VoucherLineRequest.builder()
                        .account(debitAcct)
                        .amount(req.getAmount())
                        .description(memoText)
                        .build()))
                .creditEntries(List.of(VoucherCreateRequest.VoucherLineRequest.builder()
                        .account(creditAcct)
                        .amount(req.getAmount())
                        .description(memoText)
                        .build()))
                .build());
    }

    /**
     * 지급(반환) 전표
     *   차변: 임대보증금 (depositDebitAccount  또는 "임대보증금")
     *   대변: 보통예금   (depositCreditAccount 또는 "보통예금")
     */
    private void createRefundVoucher(DepositCreateRequest req, String depositType) {
        String debitAcct  = nvl(settingsService.getDepositDebitAccount(),  "임대보증금"); // 차변
        String creditAcct = nvl(settingsService.getDepositCreditAccount(), "보통예금");   // 대변

        Contract contract = findContract(req.getContractId());
        String memoText = depositType + " 반환" + appendMemo(req.getMemo());

        voucherService.create(VoucherCreateRequest.builder()
                .voucherDate(req.getTransactionDate())
                .contractNumber(contract.getContractNumber())
                .vehicleNo(contract.getVehicleNo())
                .memo(memoText)
                .debitEntries(List.of(VoucherCreateRequest.VoucherLineRequest.builder()
                        .account(debitAcct)
                        .amount(req.getAmount())
                        .description(memoText)
                        .build()))
                .creditEntries(List.of(VoucherCreateRequest.VoucherLineRequest.builder()
                        .account(creditAcct)
                        .amount(req.getAmount())
                        .description(memoText)
                        .build()))
                .build());
    }

    // ─────────────────────────────────────────────────────────────────────
    // 내부 유틸
    // ─────────────────────────────────────────────────────────────────────

    private void validateRequest(DepositCreateRequest req) {
        if (req.getContractId() == null)
            throw new IllegalArgumentException("계약 ID는 필수입니다.");
        if (req.getAmount() == null || req.getAmount() <= 0)
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        if (req.getTransactionDate() == null)
            throw new IllegalArgumentException("거래일자는 필수입니다.");
    }

    private String resolveDepositType(DepositCreateRequest req) {
        return (req.getDepositType() == null || req.getDepositType().isBlank())
                ? "보증금" : req.getDepositType();
    }

    private Contract findContract(Long contractId) {
        return contractRepo.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다: " + contractId));
    }

    private String key(Long contractId, String depositType, String transactionType) {
        return contractId + "|" + depositType + "|" + transactionType;
    }

    private String key2(Long contractId, String depositType) {
        return contractId + "|" + depositType;
    }

    private Long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        return 0L;
    }

    private String nvl(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    private String appendMemo(String memo) {
        return (memo != null && !memo.isBlank()) ? " - " + memo : "";
    }

    private String fmt(long amount) {
        return String.format("%,d", amount);
    }
}
