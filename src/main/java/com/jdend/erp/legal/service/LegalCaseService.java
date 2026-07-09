package com.jdend.erp.legal.service;

import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.legal.dto.*;
import com.jdend.erp.legal.entity.LegalCase;
import com.jdend.erp.legal.entity.LegalProgressEntry;
import com.jdend.erp.legal.repository.LegalCaseRepository;
import com.jdend.erp.legal.repository.LegalProgressEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LegalCaseService {

    private final ContractRepository contractRepo;
    private final LegalCaseRepository caseRepo;
    private final LegalProgressEntryRepository progressRepo;

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

        return toResponse(caseRepo.save(lc), List.of());
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
        if (req.getCaseType() != null)       c.setCaseType(req.getCaseType());
        if (req.getCaseNumber() != null)     c.setCaseNumber(req.getCaseNumber());
        if (req.getRegistrationDate() != null) c.setRegistrationDate(req.getRegistrationDate());
        if (req.getLegalCostPayment() != null) c.setLegalCostPayment(req.getLegalCostPayment());
        if (req.getLegalCostRefund() != null)  c.setLegalCostRefund(req.getLegalCostRefund());
        if (req.getStatus() != null && !req.getStatus().isBlank()) c.setStatus(req.getStatus());

        return toResponse(caseRepo.save(c),
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
