package com.jdend.erp.vehicle.fine.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 과태료 고지서 일괄 업로드 기록. 파일 1개 = 1행. 성공·실패 모두 남긴다. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "traffic_fine_uploads")
public class TrafficFineUpload {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 한 번에 올린 묶음 식별자 */
  @Column(name = "batch_id", nullable = false, length = 40)
  private String batchId;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  /** 파일명에서 읽은 차량번호 */
  @Column(name = "vehicle_no", length = 30)
  private String vehicleNo;

  /** 만들어진 과태료 (실패면 null) */
  @Column(name = "fine_id")
  private Long fineId;

  /** 등록 / 확인필요 / 실패 */
  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "message", length = 1000)
  private String message;

  /** 고지서에서 읽은 원본 값(JSON) — 나중에 대조용 */
  @Column(name = "recognized_json", columnDefinition = "TEXT")
  private String recognizedJson;

  @Column(name = "uploaded_by", length = 50)
  private String uploadedBy;

  @Column(name = "uploaded_at", nullable = false)
  private LocalDateTime uploadedAt;
}
