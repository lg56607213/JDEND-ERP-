package com.jdend.erp.vehicle.service;

import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 차량 번호체계 채번 유틸 (Phase 2 - S1).
 *
 * <p>포맷 (설계 문서 v2~v4 기준):
 * <ul>
 *   <li>발주번호(10자리)   = {@code J + YYMMDD + 발주순번3} (그 날짜 내 순번, 다음날 001 리셋)</li>
 *   <li>차량관리번호(13자리) = {@code 발주번호 + [재렌트회차1][대순번2]} (최초계약=재렌트0)</li>
 * </ul>
 * 예) 2026-07-12 첫 발주 → 발주번호 {@code J260712001}
 *     · 최초계약 1대   → {@code J260712001001}
 *     · 최초계약 5대   → {@code J260712001001}~{@code J260712001005}
 *     · 1회 재렌트 1대 → {@code J260712001101}
 *
 * <p>동시성: {@link #nextOrderNo(LocalDate)}는 {@code synchronized}로 앱 인스턴스 내에서 직렬화한다.
 * 크로스 인스턴스(다중 서버) 상황까지는 이 메서드만으로는 완전 보장되지 않으나,
 * 최종 유니크 보장은 {@code vehicle_orders.vehicle_mgmt_no}의 UNIQUE 제약이 담당한다
 * (동시 발주로 같은 발주번호가 산출되면 13자리 차량관리번호 INSERT에서 제약 위반 → 저장 실패로 중복 데이터가 생기지 않음).
 */
@Component
@RequiredArgsConstructor
public class VehicleNumberGenerator {

    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");

    private final VehicleOrderRepository orderRepo;

    /**
     * 해당 날짜의 다음 발주번호(10자리)를 채번한다. 같은 날짜의 마지막 발주순번 + 1.
     */
    public synchronized String nextOrderNo(LocalDate date) {
        LocalDate d = (date != null) ? date : LocalDate.now();
        String prefix = "J" + d.format(YYMMDD); // J + 6자리 = 7자리

        int seq = orderRepo.findTopByOrderNoStartingWithOrderByOrderNoDesc(prefix)
                .map(o -> parseOrderSeq(o.getOrderNo()))
                .orElse(0) + 1;

        String candidate = prefix + String.format("%03d", seq);

        // 방어적 중복 회피: 발주번호가 이미 쓰였거나 그 발주번호로 시작하는 차량관리번호가 존재하면 순번 증가
        while (orderRepo.existsByOrderNo(candidate)
                || orderRepo.existsByVehicleMgmtNoStartingWith(candidate)) {
            seq++;
            candidate = prefix + String.format("%03d", seq);
        }

        return candidate;
    }

    /**
     * 차량관리번호(13자리) 조립. 발주번호(10) + 재렌트회차(1) + 대순번(2).
     *
     * @param orderNo      발주번호 10자리
     * @param rerentRound  재렌트 회차 (최초계약=0, 최대 9)
     * @param unitSeq      발주 내 대순번 (1..99)
     */
    public static String buildVehicleMgmtNo(String orderNo, int rerentRound, int unitSeq) {
        if (orderNo == null || orderNo.length() != 10) {
            throw new IllegalArgumentException("발주번호는 10자리여야 합니다: " + orderNo);
        }
        if (rerentRound < 0 || rerentRound > 9) {
            throw new IllegalArgumentException("재렌트 회차는 0~9만 가능합니다: " + rerentRound);
        }
        if (unitSeq < 1 || unitSeq > 99) {
            throw new IllegalArgumentException("대순번은 1~99만 가능합니다: " + unitSeq);
        }
        return orderNo + rerentRound + String.format("%02d", unitSeq);
    }

    private int parseOrderSeq(String orderNo) {
        // 발주번호 = J + YYMMDD(6) + 순번(3) = 10자리. 뒤 3자리가 순번.
        if (orderNo == null || orderNo.length() < 10) return 0;
        try {
            return Integer.parseInt(orderNo.substring(7, 10));
        } catch (Exception ignored) {
            return 0;
        }
    }
}
