package com.jdend.erp.vehicle.fine.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 과태료 고지서 일괄 업로드 · 변경부과 신청 팩스 */
public final class FineAutomationDtos {

  private FineAutomationDtos() {}

  /** 파일 1개 처리 결과. status: 등록 / 확인필요 / 실패 */
  public record UploadResult(
    Long uploadId,
    String fileName,
    String vehicleNo,
    Long fineId,
    String status,
    String message,
    LocalDateTime violationAt,
    Long amount,
    String issuingAgency,
    String contractNumber,
    String customerName
  ) {}

  public record BulkUploadResponse(String batchId, List<UploadResult> results) {}

  public record UploadLog(
    Long id,
    String batchId,
    String fileName,
    String vehicleNo,
    Long fineId,
    String status,
    String message,
    String uploadedBy,
    LocalDateTime uploadedAt
  ) {}

  public record FaxRequest(String receiverName, String receiverFax) {}

  /** state: 접수 / 전송중 / 성공 / 실패 / 오류 */
  public record FaxLog(
    Long id,
    Long fineId,
    String vehicleNo,
    String contractNumber,
    String customerName,
    String receiverName,
    String receiverFax,
    String state,
    String resultMessage,
    Integer pageCount,
    String receiptNum,
    String requestedBy,
    LocalDateTime requestedAt,
    LocalDateTime sentAt,
    LocalDateTime resultAt
  ) {}

  /** serviceConfigured: 서버에 팝빌 연동키가 설정되어 있는지(읽기 전용) */
  public record FaxSettings(
    String corpNum,
    String senderNum,
    String senderName,
    String popbillUserId,
    Boolean serviceConfigured
  ) {}
}
