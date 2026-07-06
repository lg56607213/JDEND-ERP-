package com.jdend.erp.consultation.dto;

import com.jdend.erp.consultation.entity.TaxConsultation;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxConsultationResponse {
    private Long id;
    private Long companyId;
    private String companyName;
    private String title;
    private String content;
    private boolean hasFile;
    private String fileOriginalName;
    private String fileType;
    private String answer;
    private String answeredBy;
    private LocalDateTime answeredAt;
    private String status;
    private LocalDateTime createdAt;

    public static TaxConsultationResponse from(TaxConsultation e) {
        return TaxConsultationResponse.builder()
                .id(e.getId())
                .companyId(e.getCompanyId())
                .companyName(e.getCompanyName())
                .title(e.getTitle())
                .content(e.getContent())
                .hasFile(e.getFileName() != null && !e.getFileName().isBlank())
                .fileOriginalName(e.getFileOriginalName())
                .fileType(e.getFileType())
                .answer(e.getAnswer())
                .answeredBy(e.getAnsweredBy())
                .answeredAt(e.getAnsweredAt())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
