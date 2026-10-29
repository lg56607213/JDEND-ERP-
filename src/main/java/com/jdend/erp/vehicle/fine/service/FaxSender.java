package com.jdend.erp.vehicle.fine.service;

import com.popbill.api.PopbillException;
import com.popbill.api.fax.FaxResult;
import com.popbill.api.fax.FaxServiceImp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 팝빌 팩스 API. 연동키(LinkID/SecretKey)는 서버 환경변수로만 받는다.
 * 회사별 사업자번호·발신번호는 fax_settings 에서 넘겨받는다.
 */
@Component
public class FaxSender {

  private final String linkId;
  private final String secretKey;
  private final boolean test;
  private volatile FaxServiceImp service;

  public FaxSender(@Value("${app.fax.popbill.link-id:}") String linkId,
                   @Value("${app.fax.popbill.secret-key:}") String secretKey,
                   @Value("${app.fax.popbill.test:true}") boolean test) {
    this.linkId = linkId == null ? "" : linkId.trim();
    this.secretKey = secretKey == null ? "" : secretKey.trim();
    this.test = test;
  }

  public boolean configured() {
    return !linkId.isEmpty() && !secretKey.isEmpty();
  }

  /** @return 팝빌 접수번호 */
  public String send(String corpNum, String userId, String senderNum, String receiveNum, String receiveName,
                     File file, String title) throws PopbillException {
    return service().sendFAX(corpNum, senderNum, receiveNum, receiveName, file, null, userId, false, title, null);
  }

  public FaxResult[] result(String corpNum, String receiptNum, String userId) throws PopbillException {
    return service().getFaxResult(corpNum, receiptNum, userId);
  }

  private FaxServiceImp service() {
    if (!configured()) {
      throw new IllegalStateException("팩스 연동키(POPBILL_LINK_ID/POPBILL_SECRET_KEY)가 서버에 설정되지 않았습니다.");
    }
    if (service == null) {
      synchronized (this) {
        if (service == null) {
          FaxServiceImp s = new FaxServiceImp();
          s.setLinkID(linkId);
          s.setSecretKey(secretKey);
          s.setTest(test);
          s.setIPRestrictOnOff(true);
          s.setUseStaticIP(false);
          s.setUseLocalTimeYN(true);
          service = s;
        }
      }
    }
    return service;
  }
}
