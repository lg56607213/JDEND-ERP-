package com.jdend.erp.account;

import com.jdend.erp.customer.Customer;
import com.jdend.erp.customer.CustomerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")

public class AccountController {

    private final AccountRepository repo;
    private final CustomerRepository customerRepo;

    public AccountController(AccountRepository repo, CustomerRepository customerRepo) {
        this.repo = repo;
        this.customerRepo = customerRepo;
    }

    // 전체 목록
    @GetMapping
    public List<Account> list() {
        return repo.findAll();
    }

    // 고객 기준 목록 (선택)
    @GetMapping("/by-customer/{customerId}")
    public List<Account> listByCustomer(@PathVariable Long customerId) {
        return repo.findByCustomerId(customerId);
    }

    // 1개 상세
    @GetMapping("/{id}")
    public ResponseEntity<Account> detail(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ 등록: customerNumber -> customerId 자동 세팅
    @PostMapping
    public ResponseEntity<?> create(@RequestBody AccountRequest req) {

        if (req.getCustomerNumber() == null || req.getCustomerNumber().isBlank()) {
            return ResponseEntity.badRequest().body("customerNumber는 필수입니다.");
        }

        Customer c = customerRepo.findByCustomerNumber(req.getCustomerNumber()).orElse(null);
        if (c == null) {
            return ResponseEntity.badRequest().body("해당 고객번호가 없습니다: " + req.getCustomerNumber());
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

        Account a = Account.builder()
                .customerId(c.getId())
                .customerNumber(req.getCustomerNumber())
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

    // ✅ 수정: customerNumber가 바뀌면 customerId도 다시 매핑
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody AccountRequest req) {
        return repo.findById(id).map(a -> {

            if (req.getCustomerNumber() == null || req.getCustomerNumber().isBlank()) {
                return ResponseEntity.badRequest().body("customerNumber는 필수입니다.");
            }

            Customer c = customerRepo.findByCustomerNumber(req.getCustomerNumber()).orElse(null);
            if (c == null) {
                return ResponseEntity.badRequest().body("해당 고객번호가 없습니다: " + req.getCustomerNumber());
            }

            a.setCustomerId(c.getId());
            a.setCustomerNumber(req.getCustomerNumber());
            a.setBankName(req.getBankName());
            a.setBankCode(req.getBankCode());
            a.setAccountNumber(req.getAccountNumber());
            a.setAccountHolder(req.getAccountHolder());
            a.setRelationship(req.getRelationship());
            a.setRegistrationNumber(req.getRegistrationNumber());

            return ResponseEntity.ok(repo.save(a));
        }).orElse(ResponseEntity.notFound().build());
    }

    // 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
