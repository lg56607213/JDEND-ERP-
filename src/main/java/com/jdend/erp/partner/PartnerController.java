package com.jdend.erp.partner;

import com.jdend.erp.auth.service.PermissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/partners")
public class PartnerController {

    private final PartnerRepository repo;
    private final PermissionService permissionService;
    private final PartnerNumberGenerator numberGenerator;

    public PartnerController(PartnerRepository repo, PermissionService permissionService,
                             PartnerNumberGenerator numberGenerator) {
        this.repo = repo;
        this.permissionService = permissionService;
        this.numberGenerator = numberGenerator;
    }

    @GetMapping
    public List<Partner> list() {
        return repo.findAll();
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Partner> detail(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Partner p, HttpSession session) {
        permissionService.requireManager(session);

        if (p.getCustomerName() == null || p.getCustomerName().isBlank())
            throw new IllegalArgumentException("거래처명은 필수입니다.");
        if (p.getRegistrationNumber() == null || p.getRegistrationNumber().isBlank())
            throw new IllegalArgumentException("사업자등록번호는 필수입니다.");
        if (p.getCustomerType() == null || p.getCustomerType().isBlank())
            throw new IllegalArgumentException("거래처구분은 필수입니다.");

        if (p.getRegisterDate() == null) p.setRegisterDate(LocalDate.now());

        if (p.getPartnerNumber() == null || p.getPartnerNumber().isBlank()) {
            p.setPartnerNumber(numberGenerator.next());
        }

        return ResponseEntity.ok(repo.save(p));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Partner> update(@PathVariable Long id, @RequestBody Partner req, HttpSession session) {
        permissionService.requireManager(session);
        return repo.findById(id).map(p -> {
            p.setCustomerType(req.getCustomerType());
            p.setPartnerType(req.getPartnerType());
            p.setCustomerName(req.getCustomerName());
            p.setRegistrationNumber(req.getRegistrationNumber());
            p.setPhone(req.getPhone());
            p.setCeo(req.getCeo());
            p.setBusinessType(req.getBusinessType());
            p.setBusinessItem(req.getBusinessItem());
            p.setAddress(req.getAddress());
            p.setBillAddress(req.getBillAddress());
            p.setManager(req.getManager());
            p.setManagerPhone(req.getManagerPhone());
            p.setManagerEmail(req.getManagerEmail());
            p.setBillEmail(req.getBillEmail());
            if (req.getRegisterDate() != null) p.setRegisterDate(req.getRegisterDate());
            return ResponseEntity.ok(repo.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpSession session) {
        permissionService.requireManager(session);
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
