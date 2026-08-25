package com.jdend.erp.common.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * app.storage.type 값으로 저장소를 고른다.
 *
 * <ul>
 *   <li>local — 서버 디스크 (기본값)</li>
 *   <li>s3 — S3. 자격증명은 EC2 인스턴스 역할에서 자동으로 가져온다.</li>
 * </ul>
 *
 * <p>s3로 설정했는데 버킷이 비어 있으면 로컬로 되돌린다. 설정 실수로
 * 첨부 기능 전체가 죽는 것보다는 로컬에 저장되는 편이 낫다.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    private final StorageProperties properties;

    @Bean
    public FileStorage fileStorage() {
        FileStorage local = new LocalFileStorage(properties.getLocalRoot());

        if (!properties.isS3()) {
            log.info("[Storage] 로컬 디스크 사용: {}", properties.getLocalRoot());
            return local;
        }

        String bucket = properties.getS3().getBucket();
        if (bucket == null || bucket.isBlank()) {
            log.error("[Storage] app.storage.type=s3 이지만 버킷이 비어 있습니다. 로컬 디스크로 동작합니다.");
            return local;
        }

        S3Client client = S3Client.builder()
                .region(Region.of(properties.getS3().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .httpClient(UrlConnectionHttpClient.create())  // 가벼운 동기 HTTP 클라이언트
                .build();

        log.info("[Storage] S3 사용: bucket={}, region={}, prefix='{}'",
                bucket, properties.getS3().getRegion(), properties.getS3().getPrefix());

        return new S3FileStorage(client, bucket, properties.getS3().getPrefix(),
                local, properties.getLocalRoot());
    }
}
