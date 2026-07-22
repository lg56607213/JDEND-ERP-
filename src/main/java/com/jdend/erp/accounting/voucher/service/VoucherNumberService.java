package com.jdend.erp.accounting.voucher.service;

import com.jdend.erp.accounting.voucher.entity.VoucherSequence;
import com.jdend.erp.accounting.voucher.repository.VoucherRepository;
import com.jdend.erp.accounting.voucher.repository.VoucherSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 전표번호 생성 전담 서비스.
 * voucher_sequences 테이블을 high-water mark로 사용하여
 * 전표 삭제 후에도 번호가 재사용되지 않도록 보장한다.
 */
@Service
@RequiredArgsConstructor
public class VoucherNumberService {

    private final VoucherSequenceRepository sequenceRepo;
    private final VoucherRepository voucherRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(LocalDate date) {
        String ymd = date.toString().replace("-", "");

        VoucherSequence seq = sequenceRepo.findByDatePrefixForUpdate(ymd)
                .orElseGet(() -> {
                    VoucherSequence s = new VoucherSequence();
                    s.setDatePrefix(ymd);
                    s.setLastSeq(0L);
                    return s;
                });

        long nextSeq = seq.getLastSeq() + 1;
        String candidate = ymd + String.format("%05d", nextSeq);

        // 혹시 수동 입력된 동일 번호가 존재하면 그 이후로 건너뜀
        while (voucherRepository.existsByVoucherNo(candidate)) {
            nextSeq++;
            candidate = ymd + String.format("%05d", nextSeq);
        }

        seq.setLastSeq(nextSeq);
        sequenceRepo.save(seq);
        return candidate;
    }
}
