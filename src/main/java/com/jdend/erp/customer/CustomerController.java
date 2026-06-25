package com.jdend.erp.customer;

import com.jdend.erp.common.excel.ExcelUploadResultResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/customers")

public class CustomerController {

    private final CustomerRepository repo;
    private final CustomerBulkUploadService bulkUploadService;

    public CustomerController(CustomerRepository repo, CustomerBulkUploadService bulkUploadService) {
        this.repo = repo;
        this.bulkUploadService = bulkUploadService;
    }

    @GetMapping
    public List<Customer> list() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> detail(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ 고객번호 자동 채번: C001, C002 ...
    private String nextCustomerNumber() {
        String max = repo.findMaxCustomerNumber(); // 예: "C012" 또는 null
        int next = 1;

        if (max != null && max.startsWith("C")) {
            String num = max.substring(1).replaceAll("[^0-9]", "");
            if (!num.isBlank()) {
                try {
                    next = Integer.parseInt(num) + 1;
                } catch (Exception ignored) {}
            }
        }

        // 자리수는 필요하면 늘려도 됨 (C0001 등)
        return String.format("C%03d", next);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Customer c) {
        // ✅ registerDate 기본값
        if (c.getRegisterDate() == null) c.setRegisterDate(LocalDate.now());

        // ✅ customerNumber가 비어있으면 서버가 자동 채번해서 넣어준다
        if (c.getCustomerNumber() == null || c.getCustomerNumber().isBlank()) {
            c.setCustomerNumber(nextCustomerNumber());
        }

        // ✅ customerNumber는 DB에서 NOT NULL + UNIQUE니까 무조건 있어야 함
        return ResponseEntity.ok(repo.save(c));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> update(@PathVariable Long id, @RequestBody Customer req) {
        return repo.findById(id).map(c -> {
            // customerNumber는 수정 못하게 유지(원하면 req로 받게 바꿀 수 있음)
            // c.setCustomerNumber(req.getCustomerNumber());

            c.setCustomerType(req.getCustomerType());
            c.setCustomerName(req.getCustomerName());
            c.setRegistrationNumber(req.getRegistrationNumber());
            c.setPhone(req.getPhone());
            c.setCeo(req.getCeo());
            c.setBusinessType(req.getBusinessType());
            c.setBusinessItem(req.getBusinessItem());
            c.setAddress(req.getAddress());
            c.setBillAddress(req.getBillAddress());
            c.setManager(req.getManager());
            c.setManagerPhone(req.getManagerPhone());
            c.setManagerEmail(req.getManagerEmail());
            c.setBillEmail(req.getBillEmail());
            if (req.getRegisterDate() != null) c.setRegisterDate(req.getRegisterDate());
            return ResponseEntity.ok(repo.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bulk-upload/template")
    public ResponseEntity<byte[]> bulkUploadTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customer_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bulkUploadService.template());
    }

    @PostMapping("/bulk-upload")
    public ExcelUploadResultResponse bulkUpload(@RequestParam("file") MultipartFile file) {
        return bulkUploadService.upload(file);
    }
}
