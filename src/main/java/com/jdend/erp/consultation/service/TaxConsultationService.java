package com.jdend.erp.consultation.service;

import com.jdend.erp.auth.service.AuthService;
import com.jdend.erp.consultation.dto.TaxConsultationResponse;
import com.jdend.erp.consultation.entity.TaxConsultation;
import com.jdend.erp.consultation.repository.TaxConsultationRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaxConsultationService {

    private final TaxConsultationRepository repo;

    @Value("${app.tax-upload-dir:uploads/tax}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public List<TaxConsultationResponse> list(HttpSession session) {
        String role = role(session);
        if ("ADMIN".equals(role) || "TAX_AGENT".equals(role)) {
            return repo.findAllByOrderByCreatedAtDesc().stream()
                    .map(TaxConsultationResponse::from).toList();
        }
        Long companyId = companyId(session);
        checkTaxEnabled(session);
        return repo.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(TaxConsultationResponse::from).toList();
    }

    @Transactional
    public TaxConsultationResponse create(String title, String content, MultipartFile file, HttpSession session) {
        checkTaxEnabled(session);
        Long companyId = companyId(session);
        String companyName = (String) session.getAttribute(AuthService.SESSION_COMPANY_NAME);

        if (title == null || title.isBlank()) throw new IllegalArgumentException("제목을 입력해주세요.");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("내용을 입력해주세요.");

        String storedName = null;
        String originalName = null;
        String fileType = null;

        if (file != null && !file.isEmpty()) {
            validateFile(file);
            try {
                Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
                Files.createDirectories(base);
                originalName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));
                String ext = "";
                int dot = originalName.lastIndexOf('.');
                if (dot >= 0) ext = originalName.substring(dot);
                storedName = "tax_" + UUID.randomUUID() + ext;
                Files.copy(file.getInputStream(), base.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
                fileType = file.getContentType();
            } catch (Exception e) {
                throw new RuntimeException("파일 저장 실패: " + e.getMessage());
            }
        }

        TaxConsultation saved = repo.save(TaxConsultation.builder()
                .companyId(companyId)
                .companyName(companyName)
                .title(title.trim())
                .content(content.trim())
                .fileName(storedName)
                .fileOriginalName(originalName)
                .fileType(fileType)
                .status("PENDING")
                .build());

        return TaxConsultationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TaxConsultationResponse getById(Long id, HttpSession session) {
        TaxConsultation tc = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("문의를 찾을 수 없습니다."));
        checkAccess(tc, session);
        return TaxConsultationResponse.from(tc);
    }

    @Transactional
    public TaxConsultationResponse answer(Long id, String answer, HttpSession session) {
        String role = role(session);
        if (!"ADMIN".equals(role) && !"TAX_AGENT".equals(role)) {
            throw new RuntimeException("답변 권한이 없습니다.");
        }
        if (answer == null || answer.isBlank()) throw new IllegalArgumentException("답변 내용을 입력해주세요.");

        TaxConsultation tc = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("문의를 찾을 수 없습니다."));
        tc.setAnswer(answer.trim());
        tc.setAnsweredBy((String) session.getAttribute(AuthService.SESSION_LOGIN_ID));
        tc.setAnsweredAt(LocalDateTime.now());
        tc.setStatus("ANSWERED");
        return TaxConsultationResponse.from(tc);
    }

    public Resource downloadFile(Long id, HttpSession session) {
        TaxConsultation tc = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("문의를 찾을 수 없습니다."));
        checkAccess(tc, session);
        if (tc.getFileName() == null) throw new RuntimeException("첨부파일이 없습니다.");

        Path file = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(tc.getFileName());
        if (!Files.exists(file)) throw new RuntimeException("파일을 찾을 수 없습니다.");
        return new FileSystemResource(file);
    }

    public String getOriginalFileName(Long id) {
        return repo.findById(id).map(TaxConsultation::getFileOriginalName).orElse("file");
    }

    public String getFileContentType(Long id) {
        return repo.findById(id).map(TaxConsultation::getFileType).orElse("application/octet-stream");
    }

    private void checkAccess(TaxConsultation tc, HttpSession session) {
        String role = role(session);
        if ("ADMIN".equals(role) || "TAX_AGENT".equals(role)) return;
        Long companyId = companyId(session);
        if (!tc.getCompanyId().equals(companyId)) throw new RuntimeException("접근 권한이 없습니다.");
    }

    private void checkTaxEnabled(HttpSession session) {
        String role = role(session);
        if ("ADMIN".equals(role) || "TAX_AGENT".equals(role)) return;
        Boolean enabled = (Boolean) session.getAttribute(AuthService.SESSION_TAX_CONSULTATION_ENABLED);
        if (!Boolean.TRUE.equals(enabled)) throw new RuntimeException("세무상담 서비스 권한이 없습니다.");
    }

    private Long companyId(HttpSession session) {
        Long id = (Long) session.getAttribute(AuthService.SESSION_COMPANY_ID);
        if (id == null) throw new RuntimeException("로그인이 필요합니다.");
        return id;
    }

    private String role(HttpSession session) {
        return (String) session.getAttribute(AuthService.SESSION_ROLE);
    }

    private void validateFile(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null || (!ct.startsWith("image/") && !ct.equals("application/pdf"))) {
            throw new IllegalArgumentException("이미지(JPG, PNG 등) 또는 PDF 파일만 업로드 가능합니다.");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 10MB 이하여야 합니다.");
        }
    }
}
