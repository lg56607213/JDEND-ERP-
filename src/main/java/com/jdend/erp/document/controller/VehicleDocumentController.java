package com.jdend.erp.document.controller;

import com.jdend.erp.auth.service.AuthService;
import com.jdend.erp.document.service.VehicleDocumentService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/documents")
public class VehicleDocumentController {

    private final VehicleDocumentService service;

    // ──────────────────────────────────────────────
    // GET /api/documents — 목록 조회
    // ──────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam String documentType,
            @RequestParam(required = false) String vehicleMgmtNo,
            @RequestParam(required = false) String vehicleNo,
            @RequestParam(required = false) String contractNumber,
            @RequestParam(required = false) Long insuranceId) {
        try {
            List<VehicleDocumentService.DocumentInfo> result =
                    service.list(documentType, vehicleMgmtNo, vehicleNo, contractNumber, insuranceId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "문서 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    // POST /api/documents/upload — 파일 업로드
    // ──────────────────────────────────────────────
    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam String documentType,
            @RequestParam(required = false) String vehicleMgmtNo,
            @RequestParam(required = false) String vehicleNo,
            @RequestParam(required = false) String contractNumber,
            @RequestParam(required = false) Long insuranceId,
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        try {
            String uploadedBy = (String) session.getAttribute(AuthService.SESSION_LOGIN_ID);
            if (uploadedBy == null) uploadedBy = "system";

            VehicleDocumentService.DocumentInfo result =
                    service.upload(documentType, vehicleMgmtNo, vehicleNo, contractNumber, insuranceId,
                            file, uploadedBy);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "파일 업로드 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    // GET /api/documents/{id}/view — 파일 인라인 뷰
    // ──────────────────────────────────────────────
    @GetMapping("/{id}/view")
    public ResponseEntity<?> view(@PathVariable Long id) {
        try {
            // getInfo → getFileBytes → getContentType 순서로 조회
            // getInfo 가 없으면 IllegalArgumentException 으로 404 처리됨
            VehicleDocumentService.DocumentInfo info = service.getInfo(id);
            byte[] bytes = service.getFileBytes(id);
            String contentType = service.getContentType(id);

            String filename = (info.fileName() != null && !info.fileName().isBlank())
                    ? info.fileName() : "document";

            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename*=UTF-8''" + encodedFilename);
            headers.setContentLength(bytes.length);

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "파일 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    // GET /api/documents/{id}/download — 파일 다운로드
    // ──────────────────────────────────────────────
    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable Long id) {
        try {
            VehicleDocumentService.DocumentInfo info = service.getInfo(id);
            byte[] bytes = service.getFileBytes(id);
            String contentType = service.getContentType(id);

            String filename = (info.fileName() != null && !info.fileName().isBlank())
                    ? info.fileName() : "document";

            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename*=UTF-8''" + encodedFilename);
            headers.setContentLength(bytes.length);

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "파일 다운로드 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    // DELETE /api/documents/{id} — 소프트 삭제
    // ──────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(Map.of("message", "문서가 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "문서 삭제 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    // GET /api/documents/contract-scan-status — 계약 스캔 유무 일괄 조회
    // ──────────────────────────────────────────────
    @GetMapping("/contract-scan-status")
    public ResponseEntity<?> contractScanStatus(
            @RequestParam List<String> contractNumbers) {
        try {
            Map<String, Boolean> result = service.contractScanStatus(contractNumbers);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "계약 스캔 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    // GET /api/documents/insurance-scan-status — 보험 스캔 유무 일괄 조회
    // ──────────────────────────────────────────────
    @GetMapping("/insurance-scan-status")
    public ResponseEntity<?> insuranceScanStatus(
            @RequestParam List<Long> insuranceIds) {
        try {
            Map<Long, Boolean> result = service.insuranceScanStatus(insuranceIds);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "보험 스캔 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
