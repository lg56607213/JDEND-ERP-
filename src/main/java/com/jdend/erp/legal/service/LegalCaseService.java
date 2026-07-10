package com.jdend.erp.legal.service;

import com.jdend.erp.accounting.voucher.entity.Voucher;
import com.jdend.erp.accounting.voucher.entity.VoucherLine;
import com.jdend.erp.accounting.voucher.repository.VoucherRepository;
import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.legal.dto.*;
import com.jdend.erp.legal.entity.LegalCase;
import com.jdend.erp.legal.entity.LegalProgressEntry;
import com.jdend.erp.legal.repository.LegalCaseRepository;
import com.jdend.erp.legal.repository.LegalProgressEntryRepository;
import com.jdend.erp.management.financial.repository.FinancialStatementAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class LegalCaseService {

    private final ContractRepository contractRepo;
    private final LegalCaseRepository caseRepo;
    private final LegalProgressEntryRepository progressRepo;
    private final VoucherRepository voucherRepository;
    private final FinancialStatementAccountRepository accountRepo;

    public LegalCaseResponse create(LegalCaseRequest req) {
        String cn = safe(req.getContractNumber());
        if (cn.isBlank()) throw new RuntimeException("계약번호는 필수입니다.");

        Contract c = contractRepo.findWithCustomerByContractNumber(cn)
                .orElseThrow(() -> new RuntimeException("계약 없음: " + cn));

        String customerName = c.getCustomer() != null ? c.getCustomer().getCustomerName() : null;

        LegalCase lc = LegalCase.builder()
                .contractNumber(c.getContractNumber())
                .vehicleNo(c.getVehicleNo())
                .customerName(customerName)
                .caseType(safe(req.getCaseType()))
                .caseNumber(safe(req.getCaseNumber()))
                .registrationDate(req.getRegistrationDate() != null ? req.getRegistrationDate() : LocalDate.now())
                .legalCostPayment(req.getLegalCostPayment() != null ? req.getLegalCostPayment() : 0L)
                .legalCostRefund(req.getLegalCostRefund() != null ? req.getLegalCostRefund() : 0L)
                .status(req.getStatus() != null && !req.getStatus().isBlank() ? req.getStatus() : "접수")
                .build();

        LegalCase savedCase = caseRepo.save(lc);
        // 현금주의: 법무비용이 기록되면(>0) 지출 비용 전표를 생성한다.
        if (savedCase.getLegalCostPayment() != null && savedCase.getLegalCostPayment() > 0) {
            createLegalCostVoucher(savedCase);
        }
        return toResponse(savedCase, List.of());
    }

    @Transactional(readOnly = true)
    public List<LegalCaseResponse> listByContract(String contractNumber) {
        return caseRepo.findByContractNumberOrderByIdDesc(safe(contractNumber))
                .stream()
                .map(c -> toResponse(c, progressRepo.findByLegalCaseIdOrderByProgressDateAscIdAsc(c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public LegalCaseResponse getOne(Long id) {
        LegalCase c = findCase(id);
        return toResponse(c, progressRepo.findByLegalCaseIdOrderByProgressDateAscIdAsc(id));
    }

    public LegalCaseResponse update(Long id, LegalCaseRequest req) {
        LegalCase c = findCase(id);
        long oldCost = c.getLegalCostPayment() != null ? c.getLegalCostPayment() : 0L;
        if (req.getCaseType() != null)       c.setCaseType(req.getCaseType());
        if (req.getCaseNumber() != null)     c.setCaseNumber(req.getCaseNumber());
        if (req.getRegistrationDate() != null) c.setRegistrationDate(req.getRegistrationDate());
        if (req.getLegalCostPayment() != null) c.setLegalCostPayment(req.getLegalCostPayment());
        if (req.getLegalCostRefund() != null)  c.setLegalCostRefund(req.getLegalCostRefund());
        if (req.getStatus() != null && !req.getStatus().isBlank()) c.setStatus(req.getStatus());

        LegalCase saved = caseRepo.save(c);
        long newCost = saved.getLegalCostPayment() != null ? saved.getLegalCostPayment() : 0L;
        // 법무비용이 처음으로(0 → 양수) 기록될 때만 1회 비용 전표를 생성한다(중복 방지).
        if (oldCost == 0L && newCost > 0L) {
            createLegalCostVoucher(saved);
        }

        return toResponse(saved,
                progressRepo.findByLegalCaseIdOrderByProgressDateAscIdAsc(id));
    }

    public void delete(Long id) {
        if (!caseRepo.existsById(id)) throw new RuntimeException("사건 없음: " + id);
        progressRepo.deleteByLegalCaseId(id);
        caseRepo.deleteById(id);
    }

    public LegalProgressResponse addProgress(Long caseId, LegalProgressRequest req) {
        if (!caseRepo.existsById(caseId)) throw new RuntimeException("사건 없음: " + caseId);
        String content = safe(req.getProgressContent());
        if (content.isBlank()) throw new RuntimeException("진행내역 내용은 필수입니다.");

        LegalProgressEntry entry = LegalProgressEntry.builder()
                .legalCaseId(caseId)
                .progressDate(req.getProgressDate() != null ? req.getProgressDate() : LocalDate.now())
                .progressContent(content)
                .build();

        return toProgressResponse(progressRepo.save(entry));
    }

    public void deleteProgress(Long caseId, Long entryId) {
        LegalProgressEntry e = progressRepo.findById(entryId)
                .orElseThrow(() -> new RuntimeException("진행내역 없음: " + entryId));
        if (!e.getLegalCaseId().equals(caseId)) throw new RuntimeException("사건 불일치");
        progressRepo.deleteById(entryId);
    }

    private LegalCase findCase(Long id) {
        return caseRepo.findById(id).orElseThrow(() -> new RuntimeException("사건 없음: " + id));
    }

    // 현금주의: 법무비용을 실제 지출(현금 유출)한 것으로 보고 비용 전표를 생성한다.
    // [판단 개입 — 사장 확인 대상] 별도의 '지급 실행' 이벤트가 없어, legalCostPayment가 처음 양수로
    // 기록되는 시점을 지출로 간주한다. 지급 시점을 별도 이벤트로 분리해야 하면 추후 조정.
    private void createLegalCostVoucher(LegalCase lc) {
        long amount = lc.getLegalCostPayment() != null ? lc.getLegalCostPayment() : 0L;
        if (amount <= 0) return;

        // 방어적: 필요한 계정이 해당 회사에 없으면 재무제표에서 누락/불균형이 되므로,
        // 전표를 만들지 않고 경고만 남긴다(다른 회사 데이터는 손상시키지 않음).
        String expenseAccount = "법무비용";
        String creditAccount = accountRepo.existsByAccountName("보통예금") ? "보통예금"
                : (accountRepo.existsByAccountName("현금") ? "현금" : null);
        if (!accountRepo.existsByAccountName(expenseAccount) || creditAccount == null) {
            log.warn("법무비용 전표 생략: 필요한 계정이 없습니다(법무비용 또는 보통예금/현금). legalCaseId={}", lc.getId());
            return;
        }

        LocalDate voucherDate = lc.getRegistrationDate() != null ? lc.getRegistrationDate() : LocalDate.now();
        String voucherNo = nextVoucherNo(voucherDate);

        StringBuilder memo = new StringBuilder("법적절차 비용");
        if (lc.getCaseNumber() != null && !lc.getCaseNumber().isBlank()) memo.append(" / 사건번호: ").append(lc.getCaseNumber());
        if (lc.getContractNumber() != null && !lc.getContractNumber().isBlank()) memo.append(" / 계약번호: ").append(lc.getContractNumber());

        Voucher voucher = Voucher.builder()
                .voucherNo(voucherNo)
                .voucherDate(voucherDate)
                .contractNumber(nullIfBlank(lc.getContractNumber()))
                .vehicleNo(nullIfBlank(lc.getVehicleNo()))
                .totalAmount(amount)
                .status("대기")
                .memo(memo.toString())
                .build();

        voucher.addLine(VoucherLine.builder()
                .lineType("DEBIT").accountName(expenseAccount).amount(amount)
                .description("법적절차 비용").sortOrder(1).build());
        voucher.addLine(VoucherLine.builder()
                .lineType("CREDIT").accountName(creditAccount).amount(amount)
                .description("법적절차 비용 지급").sortOrder(2).build());

        voucherRepository.save(voucher);
    }

    private String nextVoucherNo(LocalDate date) {
        long cnt = voucherRepository.countByVoucherDate(date);
        long next = cnt + 1;
        String ymd = date.toString().replace("-", "");
        return "V" + ymd + "-" + String.format("%03d", next) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private String nullIfBlank(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private LegalCaseResponse toResponse(LegalCase c, List<LegalProgressEntry> entries) {
        return LegalCaseResponse.builder()
                .id(c.getId())
                .contractNumber(c.getContractNumber())
                .vehicleNo(c.getVehicleNo())
                .customerName(c.getCustomerName())
                .caseType(c.getCaseType())
                .caseNumber(c.getCaseNumber())
                .registrationDate(c.getRegistrationDate())
                .legalCostPayment(c.getLegalCostPayment())
                .legalCostRefund(c.getLegalCostRefund())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .progressEntries(entries.stream().map(this::toProgressResponse).toList())
                .build();
    }

    private LegalProgressResponse toProgressResponse(LegalProgressEntry e) {
        return LegalProgressResponse.builder()
                .id(e.getId())
                .legalCaseId(e.getLegalCaseId())
                .progressDate(e.getProgressDate())
                .progressContent(e.getProgressContent())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
}
