package com.jdend.erp.vehicle.repository;

import com.jdend.erp.vehicle.entity.VehicleOrder;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.*;

public interface VehicleOrderRepository extends JpaRepository<VehicleOrder, Long> {

    Optional<VehicleOrder> findByVehicleMgmtNo(String vehicleMgmtNo);

    /**
     * BUG-2 수정: 발주(N대) 시 동일 vehicleMgmtNo(…000) 공유로 findByVehicleMgmtNo가
     * "Query did not return a unique result" 예외를 던지는 문제 해결용.
     * id ASC 정렬로 가장 먼저 등록된 미실행 행을 반환한다.
     */
    Optional<VehicleOrder> findFirstByVehicleMgmtNoOrderByIdAsc(String vehicleMgmtNo);

    /**
     * BUG-2 수정: 순차 차량관리번호 채번용.
     * 순수 숫자로만 이루어진 vehicleMgmtNo 중 최대값(정수 변환 기준)을 반환한다.
     * 값이 없으면 0 반환.
     */
    @Query(nativeQuery = true,
           value = "SELECT COALESCE(MAX(CAST(vehicle_mgmt_no AS UNSIGNED)), 0) " +
                   "FROM vehicle_orders " +
                   "WHERE vehicle_mgmt_no REGEXP '^[0-9]+$'")
    int findMaxSequentialMgmtNo();

    /**
     * 발주단계(…000) 관리번호를 공유하는 N대를 등록 순서대로 조회.
     * 실행 화면에서 "어느 차를 실행할지" 목록을 보여줄 때 사용한다.
     */
    List<VehicleOrder> findByVehicleMgmtNoOrderByIdAsc(String vehicleMgmtNo);

    /**
     * 같은 발주(발주번호) 안에서 이미 확정된 차량관리번호의 대순번(끝 2자리) 최대값.
     * 차량관리번호 = 발주번호(10) + 재렌트회차(1) + 대순번(2) = 13자리.
     * 발주단계(…000)와 13자리가 아닌 값은 제외한다. 없으면 0.
     */
    @Query(nativeQuery = true,
           value = "SELECT COALESCE(MAX(CAST(RIGHT(vehicle_mgmt_no, 2) AS UNSIGNED)), 0) " +
                   "FROM vehicle_orders " +
                   "WHERE order_no = :orderNo " +
                   "  AND CHAR_LENGTH(vehicle_mgmt_no) = 13 " +
                   "  AND RIGHT(vehicle_mgmt_no, 3) <> '000'")
    int findMaxUnitSeqByOrderNo(@Param("orderNo") String orderNo);

    Optional<VehicleOrder> findTopByOrderByVehicleMgmtNoDesc();

    boolean existsByVehicleMgmtNo(String vehicleMgmtNo);

    // ── 번호체계(S1) 채번용 ──
    // 같은 날짜 발주번호(J+YYMMDD+순번3) 중 가장 큰 값 → 다음 순번 계산
    Optional<VehicleOrder> findTopByOrderNoStartingWithOrderByOrderNoDesc(String prefix);

    boolean existsByOrderNo(String orderNo);

    // 발주번호(10자리)로 시작하는 차량관리번호(13자리)가 이미 있는지 방어적 확인
    boolean existsByVehicleMgmtNoStartingWith(String prefix);

    // 같은 발주(발주번호)로 묶인 N대 조회
    List<VehicleOrder> findByOrderNoOrderByVehicleMgmtNoAsc(String orderNo);

    List<VehicleOrder> findByOrderDateBetween(LocalDate start, LocalDate end);

    List<VehicleOrder> findByOrderStatus(String status);

    List<VehicleOrder> findByOrderStatusAndOrderDateBetween(String status, LocalDate start, LocalDate end);

    Optional<VehicleOrder> findByVehicleNo(String vehicleNo);

    @Query("""
        select v
        from VehicleOrder v
        where replace(replace(trim(v.vehicleNo), ' ', ''), '-', '') =
              replace(replace(trim(:vehicleNo), ' ', ''), '-', '')
    """)
    Optional<VehicleOrder> findByVehicleNoNormalized(@Param("vehicleNo") String vehicleNo);

    @Query("""
        select v
        from VehicleOrder v
        where (:kw = '' or
          lower(coalesce(v.vehicleNo, '')) like concat('%', lower(:kw), '%') or
          lower(coalesce(v.carModel, '')) like concat('%', lower(:kw), '%') or
          lower(coalesce(v.vehicleMgmtNo, '')) like concat('%', lower(:kw), '%') or
          lower(coalesce(v.makerContractNo, '')) like concat('%', lower(:kw), '%'))
        order by v.id desc
    """)
    List<VehicleOrder> searchTop500(@Param("kw") String kw);

    // 정기검사관리 조회용
    // vehicle_orders 테이블에 검사유효기간이 들어있는 차량 조회
    @Query("""
        select v
        from VehicleOrder v
        where v.inspectionStart is not null
          and v.inspectionEnd is not null
          and (:vehicleMgmtNo is null or lower(v.vehicleMgmtNo) like concat('%', lower(:vehicleMgmtNo), '%'))
          and (:vehicleNo is null or lower(coalesce(v.vehicleNo, '')) like concat('%', lower(:vehicleNo), '%'))
          and (:validStartFrom is null or v.inspectionStart >= :validStartFrom)
          and (:validEndTo is null or v.inspectionEnd <= :validEndTo)
        order by v.inspectionEnd desc, v.inspectionStart desc, v.id desc
    """)
    List<VehicleOrder> searchInspectionVehicles(
            @Param("vehicleMgmtNo") String vehicleMgmtNo,
            @Param("vehicleNo") String vehicleNo,
            @Param("validStartFrom") LocalDate validStartFrom,
            @Param("validEndTo") LocalDate validEndTo
    );
}