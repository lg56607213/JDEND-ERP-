package com.jdend.erp.vehicle.service;

import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

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

    // 같은 날짜(prefix)에 이 인스턴스가 마지막으로 발급한 발주순번.
    // v5로 vehicle_mgmt_no UNIQUE 를 제거해 DB 레벨 충돌 방어가 사라졌으므로,
    // 커밋 전이라 DB max 가 아직 반영되지 않는 동시 발주 구간을 인메모리로 메운다(단일 인스턴스 보장).
    private final Map<String, Integer> lastSeqByPrefix = new HashMap<>();

    /**
     * BUG-2 수정: 순차 차량관리번호(회사 DB 내 전체 순번) 마지막 발급값.
     * DB 커밋 전 중복 방지용 인메모리 캐시.
     */
    private volatile int lastMgmtNoSeq = 0;

    /**
     * 해당 날짜의 다음 발주번호(10자리)를 채번한다. 같은 날짜의 마지막 발주순번 + 1.
     */
    public synchronized String nextOrderNo(LocalDate date) {
        LocalDate d = (date != null) ? date : LocalDate.now();
        String prefix = "J" + d.format(YYMMDD); // J + 6자리 = 7자리

        int dbMax = orderRepo.findTopByOrderNoStartingWithOrderByOrderNoDesc(prefix)
                .map(o -> parseOrderSeq(o.getOrderNo()))
                .orElse(0);
        int cached = lastSeqByPrefix.getOrDefault(prefix, 0);
        int seq = Math.max(dbMax, cached) + 1;

        String candidate = prefix + String.format("%03d", seq);

        // 방어적 중복 회피: 발주번호가 이미 쓰였거나 그 발주번호로 시작하는 차량관리번호가 존재하면 순번 증가
        while (orderRepo.existsByOrderNo(candidate)
                || orderRepo.existsByVehicleMgmtNoStartingWith(candidate)) {
            seq++;
            candidate = prefix + String.format("%03d", seq);
        }

        lastSeqByPrefix.put(prefix, seq);
        return candidate;
    }

    /** 발주(미실행) 상태 마커: 끝 3자리 000. */
    public static final String ORDER_STAGE_SUFFIX = "000";

    /**
     * 발주(미실행) 단계 차량관리번호(13자리) = 발주번호 + "000".
     * N대 발주면 모든 행이 이 동일값을 공유(제조사계약번호로 구분). 실행(S3) 시 실번호로 확정.
     */
    public static String buildOrderStageMgmtNo(String orderNo) {
        if (orderNo == null || orderNo.length() != 10) {
            throw new IllegalArgumentException("발주번호는 10자리여야 합니다: " + orderNo);
        }
        return orderNo + ORDER_STAGE_SUFFIX;
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

    /**
     * BUG-2 수정: 회사 DB 내 전체 순번(순수 숫자 vehicleMgmtNo) 기준으로
     * 다음 차량관리번호 순번을 채번한다.
     * 결과 형식: 001, 002, ..., 099, 100, 101 (3자리 이상, 패딩은 호출 측에서 처리).
     *
     * @deprecated 번호체계 복원(2026-08)으로 더 이상 사용하지 않는다.
     *             차량관리번호는 {@link #nextUnitSeq(String)} + {@link #buildVehicleMgmtNo}로 채번한다.
     */
    @Deprecated
    public synchronized int nextSequentialMgmtNo() {
        int dbMax = orderRepo.findMaxSequentialMgmtNo();
        int next = Math.max(dbMax, lastMgmtNoSeq) + 1;
        lastMgmtNoSeq = next;
        return next;
    }

    // 발주번호별로 이 인스턴스가 마지막으로 발급한 대순번 (커밋 전 동시 실행 대비)
    private final Map<String, Integer> lastUnitSeqByOrderNo = new HashMap<>();

    /**
     * 해당 발주(발주번호) 안에서 다음 대순번(1~99)을 채번한다.
     * 이미 실행 확정된 차량들의 끝 2자리 최대값 + 1.
     *
     * <p>5대 발주라면 실행하는 순서대로 1, 2, 3, 4, 5 가 부여된다.
     */
    public synchronized int nextUnitSeq(String orderNo) {
        int dbMax = orderRepo.findMaxUnitSeqByOrderNo(orderNo);
        int cached = lastUnitSeqByOrderNo.getOrDefault(orderNo, 0);
        int next = Math.max(dbMax, cached) + 1;
        if (next > 99) {
            throw new IllegalStateException(
                    "한 발주에 실행 가능한 차량은 99대까지입니다. 발주번호=" + orderNo);
        }
        lastUnitSeqByOrderNo.put(orderNo, next);
        return next;
    }

    /**
     * 차량관리번호의 재렌트 회차(11번째 자리)를 1 올린다. 대순번은 유지된다.
     * 예) J260730001001 → J260730001101 → J260730001201
     */
    public static String nextRerentMgmtNo(String vehicleMgmtNo) {
        if (vehicleMgmtNo == null || vehicleMgmtNo.length() != 13) {
            throw new IllegalArgumentException("차량관리번호는 13자리여야 합니다: " + vehicleMgmtNo);
        }
        String orderNo = vehicleMgmtNo.substring(0, 10);
        int round;
        int unitSeq;
        try {
            round   = Integer.parseInt(vehicleMgmtNo.substring(10, 11));
            unitSeq = Integer.parseInt(vehicleMgmtNo.substring(11, 13));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("차량관리번호 형식이 올바르지 않습니다: " + vehicleMgmtNo);
        }
        if (round >= 9) {
            throw new IllegalStateException("재렌트는 9회까지만 가능합니다: " + vehicleMgmtNo);
        }
        return buildVehicleMgmtNo(orderNo, round + 1, unitSeq);
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
