package com.jdend.erp.partner;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/partners")
public class PartnerQueryController {

    private final PartnerRepository repo;

    public PartnerQueryController(PartnerRepository repo) {
        this.repo = repo;
    }

    /** 거래처 검색: 거래처번호·거래처명·연락처 부분 일치 */
    @GetMapping("/search")
    public List<Partner> search(@RequestParam(defaultValue = "") String kw) {
        return repo.searchTop200(kw);
    }

    /** 거래처번호로 단건 조회 (계좌등록 팝업 등에서 사용) */
    @GetMapping("/lookup")
    public ResponseEntity<Partner> lookup(@RequestParam String partnerNumber) {
        return repo.findByPartnerNumber(partnerNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
