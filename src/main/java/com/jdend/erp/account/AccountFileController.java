package com.jdend.erp.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins="*")
public class AccountFileController {

    private final AccountRepository accountRepo;
    private final AccountFileRepository fileRepo;

    @Value("${app.upload-dir:uploads/accounts}")
    private String uploadDir;

    public AccountFileController(AccountRepository accountRepo, AccountFileRepository fileRepo) {
        this.accountRepo = accountRepo;
        this.fileRepo = fileRepo;
    }

    // ✅ 업로드: POST /api/accounts/{id}/files
    @PostMapping(value="/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@PathVariable Long id,
                                    @RequestParam("files") List<MultipartFile> files) {
        if (!accountRepo.existsById(id)) return ResponseEntity.badRequest().body("계좌 없음 id=" + id);
        if (files == null || files.isEmpty()) return ResponseEntity.badRequest().body("파일 없음");

        try {
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(base);

            List<AccountFile> saved = new ArrayList<>();

            for (MultipartFile f : files) {
                if (f.isEmpty()) continue;

                // 10MB 제한
                if (f.getSize() > 10 * 1024 * 1024) {
                    return ResponseEntity.badRequest().body("10MB 초과: " + f.getOriginalFilename());
                }

                String original = StringUtils.cleanPath(Objects.requireNonNullElse(f.getOriginalFilename(), "file"));
                String ext = "";
                int dot = original.lastIndexOf('.');
                if (dot >= 0) ext = original.substring(dot);

                String stored = "acc_" + id + "_" + UUID.randomUUID() + ext;
                Path target = base.resolve(stored);

                Files.copy(f.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

                AccountFile meta = AccountFile.builder()
                        .accountId(id)
                        .originalName(original)
                        .storedName(stored)
                        .contentType(f.getContentType())
                        .fileSize(f.getSize())
                        .build();

                saved.add(fileRepo.save(meta));
            }

            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("업로드 실패: " + e.getMessage());
        }
    }

    // ✅ 목록: GET /api/accounts/{id}/files
    @GetMapping("/{id}/files")
    public ResponseEntity<?> list(@PathVariable Long id) {
        if (!accountRepo.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(fileRepo.findByAccountId(id));
    }

    // ✅ 다운로드: GET /api/accounts/files/{fileId}/download
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) {
        AccountFile meta = fileRepo.findById(fileId).orElse(null);
        if (meta == null) return ResponseEntity.notFound().build();

        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        File file = base.resolve(meta.getStoredName()).toFile();
        if (!file.exists()) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(file);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(meta.getOriginalName())
                .build());

        MediaType type = MediaType.APPLICATION_OCTET_STREAM;
        if (meta.getContentType() != null && !meta.getContentType().isBlank()) {
            try { type = MediaType.parseMediaType(meta.getContentType()); } catch (Exception ignored) {}
        }

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(type)
                .contentLength(file.length())
                .body(resource);
    }

    // ✅ 삭제: DELETE /api/accounts/files/{fileId}
    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<?> delete(@PathVariable Long fileId) {
        AccountFile meta = fileRepo.findById(fileId).orElse(null);
        if (meta == null) return ResponseEntity.notFound().build();

        try {
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.deleteIfExists(base.resolve(meta.getStoredName()));
            fileRepo.deleteById(fileId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("삭제 실패: " + e.getMessage());
        }
    }
}
