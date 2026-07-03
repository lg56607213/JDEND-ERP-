package com.jdend.erp.customer;

import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.common.excel.ExcelExportService;
import com.jdend.erp.common.excel.ExcelUploadResultResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customers")

public class CustomerController {

    private final CustomerRepository repo;
    private final CustomerBulkUploadService bulkUploadService;
    private final PermissionService permissionService;
    private final CustomerNumberGenerator numberGenerator;
    private final ExcelExportService excelExportService;

    public CustomerController(CustomerRepository repo, CustomerBulkUploadService bulkUploadService,
                               PermissionService permissionService, CustomerNumberGenerator numberGenerator,
                               ExcelExportService excelExportService) {
        this.repo = repo;
        this.bulkUploadService = bulkUploadService;
        this.permissionService = permissionService;
        this.numberGenerator = numberGenerator;
        this.excelExportService = excelExportService;
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

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Customer c) {
        // ✅ registerDate 기본값
        if (c.getRegisterDate() == null) c.setRegisterDate(LocalDate.now());

        // ✅ customerNumber가 비어있으면 서버가 자동 채번해서 넣어준다
        if (c.getCustomerNumber() == null || c.getCustomerNumber().isBlank()) {
            c.setCustomerNumber(numberGenerator.next());
        }

        // ✅ customerNumber는 DB에서 NOT NULL + UNIQUE니까 무조건 있어야 함
        return ResponseEntity.ok(repo.save(c));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> update(@PathVariable Long id, @RequestBody Customer req, HttpSession session) {
        permissionService.requireManager(session);
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
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpSession session) {
        permissionService.requireManager(session);
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        String[] headers = {"고객번호", "고객유형", "고객명", "주민/사업자번호", "연락처", "대표자",
                "업종", "업태", "주소", "청구지주소", "담당자", "담당자연락처", "담당자이메일", "청구이메일", "등록일"};
        List<Object[]> rows = repo.findAll().stream().map(c -> new Object[]{
                c.getCustomerNumber(), c.getCustomerType(), c.getCustomerName(),
                c.getRegistrationNumber(), c.getPhone(), c.getCeo(),
                c.getBusinessType(), c.getBusinessItem(), c.getAddress(), c.getBillAddress(),
                c.getManager(), c.getManagerPhone(), c.getManagerEmail(), c.getBillEmail(),
                c.getRegisterDate()
        }).collect(Collectors.toList());
        byte[] data = excelExportService.build("고객목록", headers, rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''customers.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
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
