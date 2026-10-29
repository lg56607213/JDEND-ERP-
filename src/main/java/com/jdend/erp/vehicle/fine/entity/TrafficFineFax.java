package com.jdend.erp.vehicle.fine.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 과태료 변경부과 신청 팩스 발송 기록. 보낼 때마다 1행, 결과는 팝빌 조회로 갱신한다. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "traffic_fine_faxes")
public class TrafficFineFax {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "fine_id", nullable = false)
  private Long fineId;

  @Column(name = "vehicle_no", length = 30)
  private String vehicleNo;

  @Column(name = "contract_number", length = 50)
  private String contractNumber;

  @Column(name = "customer_name", length = 100)
  private String customerName;

  @Column(name = "receiver_name", nullable = false, length = 100)
  private String receiverName;

  @Column(name = "receiver_fax", nullable = false, length = 30)
  private String receiverFax;

  /** 접수 / 전송중 / 성공 / 실패 / 오류 */
  @Column(name = "state", nullable = false, length = 20)
  private String state;

  @Column(name = "result_message", length = 500)
  private String resultMessage;

  @Column(name = "page_count")
  private Integer pageCount;

  /** 팝빌 접수번호 */
  @Column(name = "receipt_num", length = 40)
  private String receiptNum;

  @Column(name = "requested_by", length = 50)
  private String requestedBy;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  /** 실제 전송 시각(팝빌 sendDT) */
  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  /** 결과 수신 시각(팝빌 resultDT) */
  @Column(name = "result_at")
  private LocalDateTime resultAt;
}
