package com.jdend.erp.partner;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 거래처번호 자동 채번 (P00001, P00002 ...). PartnerController가 공유한다. */
@Component
@RequiredArgsConstructor
public class PartnerNumberGenerator {

    private final PartnerRepository repo;

    public synchronized String next() {
        String max = repo.findMaxPartnerNumber(); // 예: "P00012" 또는 null
        int next = 1;

        if (max != null && max.startsWith("P")) {
            String num = max.substring(1).replaceAll("[^0-9]", "");
            if (!num.isBlank()) {
                try {
                    next = Integer.parseInt(num) + 1;
                } catch (Exception ignored) {}
            }
        }

        return String.format("P%05d", next);
    }
}
