package com.jdend.erp.document.service;

import com.jdend.erp.document.entity.VehicleDocument;
import com.jdend.erp.document.repository.VehicleDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleDocumentService {

    private final VehicleDocumentRepository repository;

    @Value("${app.upload-dir:uploads/accounts}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");

    // ──────────────────────────────────────────────
    // Inner DTO
    // ──────────────────────────────────────────────
    public record DocumentInfo(
            Long id,
            String documentType,
            String vehicleMgmtNo,
            String vehicleNo,
            String contractNumber,
            Long insuranceId,
            String fileName,
            Long fileSize,
            java.time.LocalDateTime uploadedAt,
            String viewUrl
    ) {}

    // ──────────────────────────────────────────────
    // 목록 조회
    // ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DocumentInfo> list(String documentType,
                                   String vehicleMgmtNo,
                                   String vehicleNo,
                                   String contractNumber,
                                   Long insuranceId) {
        List<VehicleDocument> docs;

        if (vehicleMgmtNo != null && !vehicleMgmtNo.isBlank()) {
            docs = repository.findByVehicleMgmtNoAndDeletedYnOrderByUploadedAtDesc(vehicleMgmtNo, "N");
        } else if (vehicleNo != null && !vehicleNo.isBlank()) {
            docs = repository.findByVehicleNoAndDeletedYnOrderByUploadedAtDesc(vehicleNo, "N");
        } else if (contractNumber != null && !contractNumber.isBlank()) {
            docs = repository.findByContractNumberAndDeletedYnOrderByUploadedAtDesc(contractNumber, "N");
        } else if (insuranceId != null) {
            docs = repository.findByInsuranceIdAndDeletedYnOrderByUploadedAtDesc(insuranceId, "N");
        } else if (documentType != null && !documentType.isBlank()) {
            // 키 없이 documentType만 있으면 해당 종류 전체 반환
            docs = repository.findByDocumentTypeAndDeletedYnOrderByUploadedAtDesc(documentType, "N");
            return docs.stream().map(this::toDocumentInfo).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }

        return docs.stream()
                .filter(d -> documentType == null || documentType.isBlank() || documentType.equals(d.getDocumentType()))
                .map(this::toDocumentInfo)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // 파일 업로드
    // ──────────────────────────────────────────────
    @Transactional
    public DocumentInfo upload(String documentType,
                               String vehicleMgmtNo,
                               String vehicleNo,
                               String contractNumber,
                               Long insuranceId,
                               MultipartFile file,
                               String uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        if (documentType == null || documentType.isBlank()) {
            throw new IllegalArgumentException("documentType은 필수입니다.");
        }

        // 확장자 검증
        String originalFilename = file.getOriginalFilename();
        String ext = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. (허용: pdf, jpg, jpeg, png)");
        }

        // 크기 검증
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
        }

        // 저장 경로 구성: {uploadDir}/documents/{documentType}/{yyyy-MM}/{uuid}.{ext}
        String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String uuid = UUID.randomUUID().toString();
        String savedFileName = uuid + "." + ext.toLowerCase();

        Path dirPath = Path.of(uploadDir)
                .resolve("documents")
                .resolve(documentType)
                .resolve(yearMonth);

        try {
            Files.createDirectories(dirPath);
            Path filePath = dirPath.resolve(savedFileName);
            Files.write(filePath, file.getBytes());

            VehicleDocument doc = VehicleDocument.builder()
                    .documentType(documentType)
                    .vehicleMgmtNo(vehicleMgmtNo)
                    .vehicleNo(vehicleNo)
                    .contractNumber(contractNumber)
                    .insuranceId(insuranceId)
                    .fileName(originalFilename != null ? originalFilename : savedFileName)
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .uploadedBy(uploadedBy != null ? uploadedBy : "system")
                    .uploadedAt(LocalDateTime.now())
                    .deletedYn("N")
                    .build();

            VehicleDocument saved = repository.save(doc);
            return toDocumentInfo(saved);

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────
    // 파일 바이트 조회 (뷰용)
    // ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] getFileBytes(Long id) {
        VehicleDocument doc = repository.findById(id)
                .filter(d -> "N".equals(d.getDeletedYn()))
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다. id=" + id));

        try {
            return Files.readAllBytes(Path.of(doc.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("파일을 읽을 수 없습니다: " + e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────
    // Content-Type 반환
    // ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public String getContentType(Long id) {
        VehicleDocument doc = repository.findById(id)
                .filter(d -> "N".equals(d.getDeletedYn()))
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다. id=" + id));

        String ext = extractExtension(doc.getFileName()).toLowerCase();
        return switch (ext) {
            case "pdf"  -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png"  -> "image/png";
            default     -> "application/octet-stream";
        };
    }

    // ──────────────────────────────────────────────
    // 단건 조회 (뷰 엔드포인트용)
    // ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public DocumentInfo getInfo(Long id) {
        VehicleDocument doc = repository.findById(id)
                .filter(d -> "N".equals(d.getDeletedYn()))
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다. id=" + id));
        return toDocumentInfo(doc);
    }

    // ──────────────────────────────────────────────
    // 소프트 삭제
    // ──────────────────────────────────────────────
    @Transactional
    public void delete(Long id) {
        VehicleDocument doc = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다. id=" + id));
        doc.setDeletedYn("Y");
        repository.save(doc);
    }

    // ──────────────────────────────────────────────
    // 계약 스캔 유무 일괄 조회
    // ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Boolean> contractScanStatus(List<String> contractNumbers) {
        if (contractNumbers == null || contractNumbers.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> scanned = new HashSet<>(
                repository.findContractNumbersWithDocument(contractNumbers));

        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String cn : contractNumbers) {
            result.put(cn, scanned.contains(cn));
        }
        return result;
    }

    // ──────────────────────────────────────────────
    // 보험 스캔 유무 일괄 조회
    // ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<Long, Boolean> insuranceScanStatus(List<Long> insuranceIds) {
        if (insuranceIds == null || insuranceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> scanned = new HashSet<>(
                repository.findInsuranceIdsWithDocument(insuranceIds));

        Map<Long, Boolean> result = new LinkedHashMap<>();
        for (Long insId : insuranceIds) {
            result.put(insId, scanned.contains(insId));
        }
        return result;
    }

    // ──────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────
    private DocumentInfo toDocumentInfo(VehicleDocument doc) {
        return new DocumentInfo(
                doc.getId(),
                doc.getDocumentType(),
                doc.getVehicleMgmtNo(),
                doc.getVehicleNo(),
                doc.getContractNumber(),
                doc.getInsuranceId(),
                doc.getFileName(),
                doc.getFileSize(),
                doc.getUploadedAt(),
                "/api/documents/" + doc.getId() + "/view"
        );
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
