package com.jdend.erp.accounting.voucher.service;

import com.jdend.erp.accounting.voucher.dto.*;
import com.jdend.erp.accounting.voucher.entity.Voucher;
import com.jdend.erp.accounting.voucher.entity.VoucherLine;
import com.jdend.erp.accounting.voucher.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;

    @Transactional(readOnly = true)
    public String nextVoucherNo(LocalDate date) {
        long cnt = voucherRepository.countByVoucherDate(date);
        long next = cnt + 1;
        String ymd = date.toString().replace("-", "");
        String voucherNo = "V" + ymd + "-" + String.format("%03d", next);
        while (voucherRepository.existsByVoucherNo(voucherNo)) {
            next++;
            voucherNo = "V" + ymd + "-" + String.format("%03d", next);
        }
        return voucherNo;
    }

    @Transactional
    public VoucherCreateResponse create(VoucherCreateRequest req) {
        if (req.getVoucherDate() == null) {
            throw new IllegalArgumentException("voucherDate(전표일자)는 필수입니다.");
        }

        String voucherNo = (req.getVoucherNo() == null || req.getVoucherNo().isBlank())
                ? nextVoucherNo(req.getVoucherDate())
                : req.getVoucherNo().trim();

        if (voucherRepository.existsByVoucherNo(voucherNo)) {
            throw new IllegalArgumentException("이미 존재하는 전표번호입니다: " + voucherNo);
        }

        long debitSum = sum(req, true);
        long creditSum = sum(req, false);

        if (debitSum <= 0 || creditSum <= 0) {
            throw new IllegalArgumentException("차변/대변 금액은 0보다 커야 합니다.");
        }
        if (debitSum != creditSum) {
            throw new IllegalArgumentException("차변과 대변 합계가 일치하지 않습니다. (차변=" + debitSum + ", 대변=" + creditSum + ")");
        }

        Voucher voucher = Voucher.builder()
                .voucherNo(voucherNo)
                .voucherDate(req.getVoucherDate())
                .contractNumber(blankToNull(req.getContractNumber()))
                .vehicleNo(blankToNull(req.getVehicleNo()))
                .vehicleMgmtNo(blankToNull(req.getVehicleMgmtNo()))
                .totalAmount(debitSum)
                .status("대기")
                .memo(blankToNull(req.getMemo()))
                .build();

        int sort = 1;

        if (req.getDebitEntries() != null) {
            for (var lineReq : req.getDebitEntries()) {
                if (lineReq == null) continue;
                if (isBlank(lineReq.getAccount())) continue;

                long amt = lineReq.getAmount() == null ? 0 : lineReq.getAmount();
                if (amt <= 0) continue;

                voucher.addLine(VoucherLine.builder()
                        .lineType("DEBIT")
                        .accountName(lineReq.getAccount().trim())
                        .amount(amt)
                        .description(blankToNull(lineReq.getDescription()))
                        .sortOrder(sort++)
                        .build());
            }
        }

        if (req.getCreditEntries() != null) {
            for (var lineReq : req.getCreditEntries()) {
                if (lineReq == null) continue;
                if (isBlank(lineReq.getAccount())) continue;

                long amt = lineReq.getAmount() == null ? 0 : lineReq.getAmount();
                if (amt <= 0) continue;

                voucher.addLine(VoucherLine.builder()
                        .lineType("CREDIT")
                        .accountName(lineReq.getAccount().trim())
                        .amount(amt)
                        .description(blankToNull(lineReq.getDescription()))
                        .sortOrder(sort++)
                        .build());
            }
        }

        Voucher saved = voucherRepository.save(voucher);

        return VoucherCreateResponse.builder()
                .id(saved.getId())
                .voucherNo(saved.getVoucherNo())
                .build();
    }

    /**
     * 전표승인 화면용
     * - 전표 1건 안의 debit/credit 라인을 각각 순서대로 매칭해서 여러 줄로 펼쳐줌
     * - 첫 줄만 체크박스/일자/전표번호/상태 표시
     */
    @Transactional(readOnly = true)
    public List<VoucherApprovalRowResponse> listForApproval(LocalDate date, String status) {
        return voucherRepository.searchForApproval(date, status).stream()
                .flatMap(v -> {
                    List<VoucherLine> lines = v.getLines().stream()
                            .sorted(Comparator.comparing(VoucherLine::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                            .toList();

                    List<VoucherLine> debitLines = lines.stream()
                            .filter(line -> "DEBIT".equalsIgnoreCase(line.getLineType()))
                            .toList();

                    List<VoucherLine> creditLines = lines.stream()
                            .filter(line -> "CREDIT".equalsIgnoreCase(line.getLineType()))
                            .toList();

                    int rowCount = Math.max(Math.max(debitLines.size(), creditLines.size()), 1);

                    return java.util.stream.IntStream.range(0, rowCount)
                            .mapToObj(i -> {
                                VoucherLine debit = i < debitLines.size() ? debitLines.get(i) : null;
                                VoucherLine credit = i < creditLines.size() ? creditLines.get(i) : null;

                                return VoucherApprovalRowResponse.builder()
                                        .id(v.getId())
                                        .voucherDate(v.getVoucherDate())
                                        .voucherNo(v.getVoucherNo())

                                        .debitAccount(debit != null ? nvl(debit.getAccountName()) : "")
                                        .debitAmount(debit != null ? debit.getAmount() : null)
                                        .debitDescription(debit != null ? nvl(debit.getDescription()) : "")

                                        .creditAccount(credit != null ? nvl(credit.getAccountName()) : "")
                                        .creditAmount(credit != null ? credit.getAmount() : null)
                                        .creditDescription(credit != null ? nvl(credit.getDescription()) : "")

                                        .status(v.getStatus())
                                        .showMain(i == 0)
                                        .build();
                            });
                })
                .toList();
    }

    @Transactional
    public int approveByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        return voucherRepository.approveByIds(ids);
    }

    @Transactional
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        voucherRepository.deleteAllById(ids);
        return ids.size();
    }

    private long sum(VoucherCreateRequest req, boolean debit) {
        long s = 0;
        var list = debit ? req.getDebitEntries() : req.getCreditEntries();
        if (list == null) return 0;

        for (var r : list) {
            if (r == null) continue;
            if (isBlank(r.getAccount())) continue;

            long amt = r.getAmount() == null ? 0 : r.getAmount();
            if (amt > 0) s += amt;
        }
        return s;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}