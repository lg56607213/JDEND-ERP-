package com.jdend.erp.document.repository;

import com.jdend.erp.document.entity.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {

    List<VehicleDocument> findByVehicleMgmtNoAndDeletedYnOrderByUploadedAtDesc(
            String vehicleMgmtNo, String deletedYn);

    List<VehicleDocument> findByVehicleNoAndDeletedYnOrderByUploadedAtDesc(
            String vehicleNo, String deletedYn);

    List<VehicleDocument> findByContractNumberAndDeletedYnOrderByUploadedAtDesc(
            String contractNumber, String deletedYn);

    List<VehicleDocument> findByInsuranceIdAndDeletedYnOrderByUploadedAtDesc(
            Long insuranceId, String deletedYn);

    boolean existsByContractNumberAndDeletedYn(String contractNumber, String deletedYn);

    boolean existsByInsuranceIdAndDeletedYn(Long insuranceId, String deletedYn);

    boolean existsByVehicleMgmtNoAndDocumentTypeAndDeletedYn(
            String vehicleMgmtNo, String documentType, String deletedYn);

    /** documentType 전체 조회 — vehicleMgmtNo 등 키 없이 종류별 전체 조회용 */
    List<VehicleDocument> findByDocumentTypeAndDeletedYnOrderByUploadedAtDesc(
            String documentType, String deletedYn);

    @Query("select d.contractNumber from VehicleDocument d " +
           "where d.contractNumber in :numbers and d.deletedYn = 'N'")
    List<String> findContractNumbersWithDocument(@Param("numbers") List<String> numbers);

    @Query("select d.insuranceId from VehicleDocument d " +
           "where d.insuranceId in :ids and d.deletedYn = 'N'")
    List<Long> findInsuranceIdsWithDocument(@Param("ids") List<Long> ids);
}
