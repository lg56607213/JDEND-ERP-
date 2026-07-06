package com.jdend.erp.accounting.payable.service;

import com.jdend.erp.accounting.payable.dto.PayableBulkPayRequest;
import com.jdend.erp.accounting.payable.dto.PayableLineResponse;
import com.jdend.erp.accounting.voucher.dto.VoucherCreateRequest;
import com.jdend.erp.accounting.voucher.dto.VoucherCreateResponse;
import com.jdend.erp.accounting.voucher.entity.VoucherLine;
import com.jdend.erp.accounting.voucher.repository.VoucherLineRepository;
import com.jdend.erp.accounting.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayableService {

    private final VoucherLineRepository lineRepo;
    private final VoucherService voucherService;

    @Transactional(readOnly = true)
    public List<PayableLineResponse> search(LocalDate startDate, LocalDate endDate, String accountName) {
        String acct = (accountName == null || accountName.isBlank()) ? "" : accountName.trim();
        return lineRepo.findPayables(startDate, endDate, acct).stream()
                .map(l -> PayableLineResponse.builder()
                        .id(l.getId())
                        .voucherNo(l.getVoucher().getVoucherNo())
                        .voucherDate(l.getVoucher().getVoucherDate())
                        .accountName(l.getAccountName())
                        .amount(l.getAmount())
                        .description(l.getDescription())
                        .memo(l.getVoucher().getMemo())
                        .contractNumber(l.getVoucher().getContractNumber())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> accountNames() {
        return lineRepo.findPayableAccountNames();
    }

    @Transactional
    public VoucherCreateResponse bulkPay(PayableBulkPayRequest req) {
        if (req.getLineIds() == null || req.getLineIds().isEmpty()) {
            throw new IllegalArgumentException("지급할 항목을 선택해주세요.");
        }

        List<VoucherLine> lines = lineRepo.findAllById(req.getLineIds());
        if (lines.isEmpty()) throw new IllegalArgumentException("선택된 항목을 찾을 수 없습니다.");

        // 이미 지급된 항목 체크
        List<VoucherLine> alreadyPaid = lines.stream().filter(VoucherLine::isPaid).toList();
        if (!alreadyPaid.isEmpty()) {
            throw new IllegalArgumentException("이미 지급 처리된 항목이 포함되어 있습니다.");
        }

        LocalDate payDate = req.getPayDate() != null ? req.getPayDate() : LocalDate.now();
        String bankAccount = (req.getBankAccount() == null || req.getBankAccount().isBlank())
                ? "보통예금" : req.getBankAccount().trim();

        long totalAmount = lines.stream().mapToLong(VoucherLine::getAmount).sum();

        // 차변: 각 미지급 계정 (미지급금 → 감소)
        List<VoucherCreateRequest.VoucherLineRequest> debits = lines.stream()
                .map(l -> VoucherCreateRequest.VoucherLineRequest.builder()
                        .account(l.getAccountName())
                        .amount(l.getAmount())
                        .description(l.getDescription())
                        .build())
                .toList();

        // 대변: 보통예금 합계
        List<VoucherCreateRequest.VoucherLineRequest> credits = List.of(
                VoucherCreateRequest.VoucherLineRequest.builder()
                        .account(bankAccount)
                        .amount(totalAmount)
                        .description(req.getMemo())
                        .build()
        );

        VoucherCreateResponse response = voucherService.create(
                VoucherCreateRequest.builder()
                        .voucherDate(payDate)
                        .memo(req.getMemo())
                        .debitEntries(debits)
                        .creditEntries(credits)
                        .build()
        );

        // 지급 완료 표시
        for (VoucherLine line : lines) {
            line.setPaid(true);
            line.setPaidAt(payDate);
            line.setPaidVoucherNo(response.getVoucherNo());
        }
        lineRepo.saveAll(lines);

        return response;
    }
}
