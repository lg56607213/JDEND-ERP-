package com.jdend.erp.vehicle.fine.entity;

import jakarta.persistence.*;
import lombok.*;

/** 회사별 팩스 발신 설정(팝빌 회원 사업자번호·발신번호). 회사 DB 마다 1행. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "fax_settings")
public class FaxSetting {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 팝빌 회원 사업자번호(하이픈 없이 10자리) */
  @Column(name = "corp_num", length = 10)
  private String corpNum;

  @Column(name = "sender_num", length = 20)
  private String senderNum;

  @Column(name = "sender_name", length = 100)
  private String senderName;

  @Column(name = "popbill_user_id", length = 50)
  private String popbillUserId;
}
