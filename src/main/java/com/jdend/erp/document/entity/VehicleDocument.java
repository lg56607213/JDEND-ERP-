package com.jdend.erp.document.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_type", length = 20, nullable = false)
    private String documentType;

    @Column(name = "vehicle_mgmt_no", length = 20)
    private String vehicleMgmtNo;

    @Column(name = "vehicle_no", length = 30)
    private String vehicleNo;

    @Column(name = "contract_number", length = 50)
    private String contractNumber;

    @Column(name = "insurance_id")
    private Long insuranceId;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "file_path", length = 500, nullable = false)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "deleted_yn", length = 1)
    @Builder.Default
    private String deletedYn = "N";
}
