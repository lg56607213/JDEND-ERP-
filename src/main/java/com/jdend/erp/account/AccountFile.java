package com.jdend.erp.account;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name="account_files")
public class AccountFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="account_id", nullable=false)
    private Long accountId;

    @Column(name="original_name", nullable=false, length=255)
    private String originalName;

    @Column(name="stored_name", nullable=false, length=255)
    private String storedName;

    @Column(name="content_type", length=100)
    private String contentType;

    @Column(name="file_size")
    private Long fileSize;

    @Column(name="created_at", insertable=false, updatable=false)
    private LocalDateTime createdAt;
}
