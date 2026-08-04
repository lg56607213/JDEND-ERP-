package com.jdend.erp.partner;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/partner-accounts")
public class PartnerAccountController {

    private final PartnerAccountRepository repo;
    private final PartnerRepository partnerRepo;

    public PartnerAccountController(PartnerAccountRepository repo, PartnerRepository partnerRepo) {
        this.repo = repo;
        this.partnerRepo = partnerRepo;
    }

    /** 전체 목록 또는 거래처번호 기준 필터 */
    @GetMapping
    public List<PartnerAccount> list(@RequestParam(required = false) String partnerNumber) {
        if (partnerNumber != null && !partnerNumber.isBlank()) {
            return repo.findByPartnerNumber(partnerNumber);
        }
        return repo.findAll();
    }

    /** 거래처 기준 목록 */
    @GetMapping("/by-partner/{partnerId}")
    public List<PartnerAccount> listByPartner(@PathVariable Long partnerId) {
        return repo.findByPartnerId(partnerId);
    }

    /** 단건 상세 */
    @GetMapping("/{id}")
    public ResponseEntity<PartnerAccount> detail(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 등록: partnerNumber → partnerId 자동 세팅 */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody PartnerAccountRequest req) {

        if (req.getPartnerNumber() == null || req.getPartnerNumber().isBlank()) {
            return ResponseEntity.badRequest().body("partnerNumber는 필수입니다.");
        }

        Partner p = partnerRepo.findByPartnerNumber(req.getPartnerNumber()).orElse(null);
        if (p == null) {
            return ResponseEntity.badRequest().body("해당 거래처번호가 없습니다: " + req.getPartnerNumber());
        }

        if (req.getBankName() == null || req.getBankName().isBlank()) {
            return ResponseEntity.badRequest().body("bankName은 필수입니다.");
        }
        if (req.getAccountNumber() == null || req.getAccountNumber().isBlank()) {
            return ResponseEntity.badRequest().body("accountNumber는 필수입니다.");
        }
        if (req.getAccountHolder() == null || req.getAccountHolder().isBlank()) {
            return ResponseEntity.badRequest().body("accountHolder는 필수입니다.");
        }

        PartnerAccount a = PartnerAccount.builder()
                .partnerId(p.getId())
                .partnerNumber(req.getPartnerNumber())
                .bankName(req.getBankName())
                .bankCode(req.getBankCode())
                .accountNumber(req.getAccountNumber())
                .accountHolder(req.getAccountHolder())
                .relationship(req.getRelationship())
                .registrationNumber(req.getRegistrationNumber())
                .registerDate(LocalDate.now())
                .build();

        return ResponseEntity.ok(repo.save(a));
    }

    /** 수정: partnerNumber가 바뀌면 partnerId도 재매핑 */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PartnerAccountRequest req) {
        return repo.findById(id).map(a -> {

            if (req.getPartnerNumber() == null || req.getPartnerNumber().isBlank()) {
                return ResponseEntity.badRequest().body("partnerNumber는 필수입니다.");
            }

            Partner p = partnerRepo.findByPartnerNumber(req.getPartnerNumber()).orElse(null);
            if (p == null) {
                return ResponseEntity.badRequest().body("해당 거래처번호가 없습니다: " + req.getPartnerNumber());
            }

            a.setPartnerId(p.getId());
            a.setPartnerNumber(req.getPartnerNumber());
            a.setBankName(req.getBankName());
            a.setBankCode(req.getBankCode());
            a.setAccountNumber(req.getAccountNumber());
            a.setAccountHolder(req.getAccountHolder());
            a.setRelationship(req.getRelationship());
            a.setRegistrationNumber(req.getRegistrationNumber());

            return ResponseEntity.ok(repo.save(a));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
