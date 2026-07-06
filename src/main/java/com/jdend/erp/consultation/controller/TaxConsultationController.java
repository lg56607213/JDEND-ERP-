package com.jdend.erp.consultation.controller;

import com.jdend.erp.consultation.dto.TaxConsultationResponse;
import com.jdend.erp.consultation.service.TaxConsultationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tax-consultations")
@RequiredArgsConstructor
public class TaxConsultationController {

    private final TaxConsultationService service;

    @GetMapping
    public List<TaxConsultationResponse> list(HttpSession session) {
        return service.list(session);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpSession session) {
        try {
            return ResponseEntity.ok(service.create(title, content, file, session));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, HttpSession session) {
        try {
            return ResponseEntity.ok(service.getById(id, session));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/answer")
    public ResponseEntity<?> answer(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        try {
            String answerText = body.get("answer");
            return ResponseEntity.ok(service.answer(id, answerText, session));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, HttpSession session) {
        try {
            Resource resource = service.downloadFile(id, session);
            String originalName = service.getOriginalFileName(id);
            String contentType = service.getFileContentType(id);

            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            try { mediaType = MediaType.parseMediaType(contentType); } catch (Exception ignored) {}

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(originalName).build().toString())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
