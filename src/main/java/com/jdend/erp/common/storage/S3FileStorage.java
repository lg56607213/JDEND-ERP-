package com.jdend.erp.common.storage;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.file.Path;

/**
 * S3에 저장한다.
 *
 * <p>기존 레코드에 절대경로가 들어 있을 수 있는데(로컬 저장 시절), 그런 키는
 * S3에 없으므로 로컬 디스크로 넘겨 읽는다. 이렇게 해두면 S3로 전환한 뒤에도
 * 아직 옮기지 않은 파일이 그대로 열린다.</p>
 */
@Slf4j
public class S3FileStorage implements FileStorage {

    private final S3Client client;
    private final String bucket;
    private final String prefix;
    private final FileStorage localFallback;
    private final Path localRoot;

    public S3FileStorage(S3Client client, String bucket, String prefix,
                         FileStorage localFallback, String localRoot) {
        this.client = client;
        this.bucket = bucket;
        this.prefix = normalizePrefix(prefix);
        this.localFallback = localFallback;
        this.localRoot = Path.of(localRoot).toAbsolutePath().normalize();
    }

    /**
     * 예전 레코드의 절대경로를 S3 키로 바꾼다.
     * {@code /home/ubuntu/app/uploads/accounts/documents/x.pdf} → {@code accounts/documents/x.pdf}
     * 로컬 루트 밖의 경로면 변환하지 않는다(그 경우에만 디스크로 폴백).
     */
    private String toRelativeKey(String key) {
        if (key == null) return null;
        Path p = Path.of(key);
        if (!p.isAbsolute()) return key;
        Path norm = p.normalize();
        if (norm.startsWith(localRoot)) {
            return localRoot.relativize(norm).toString().replace('\\', '/');
        }
        return null;   // 변환 불가 → 디스크에서 읽는다
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        PutObjectRequest.Builder req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey(key));
        if (contentType != null && !contentType.isBlank()) {
            req.contentType(contentType);
        }
        client.putObject(req.build(), RequestBody.fromBytes(content));
    }

    @Override
    public byte[] get(String key) {
        String mapped = toRelativeKey(key);
        if (mapped == null) {
            return localFallback.get(key);   // 로컬 루트 밖의 옛 경로
        }
        key = mapped;
        try {
            return client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey(key))
                    .build()).asByteArray();
        } catch (NoSuchKeyException e) {
            // S3로 옮기기 전에 저장된 파일이면 디스크에 남아 있다.
            if (localFallback.exists(key)) {
                log.info("[Storage] S3에 없어 로컬에서 읽음: {}", key);
                return localFallback.get(key);
            }
            throw new RuntimeException("파일을 찾을 수 없습니다: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        String mapped = toRelativeKey(key);
        if (mapped == null) {
            return localFallback.exists(key);
        }
        key = mapped;
        try {
            client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey(key))
                    .build());
            return true;
        } catch (NoSuchKeyException | NoSuchBucketException e) {
            return localFallback.exists(key);
        }
    }

    @Override
    public void delete(String key) {
        String mapped = toRelativeKey(key);
        if (mapped == null) {
            localFallback.delete(key);
            return;
        }
        String originalKey = key;
        key = mapped;
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey(key))
                    .build());
        } catch (Exception e) {
            log.warn("[Storage] S3 삭제 실패 {}: {}", key, e.getMessage());
        }
        localFallback.delete(originalKey); // 이전에 로컬에 남아 있던 사본도 정리
    }

    @Override
    public String describe(String key) {
        String mapped = toRelativeKey(key);
        return mapped == null ? localFallback.describe(key)
                              : "s3://" + bucket + "/" + objectKey(mapped);
    }

    private String objectKey(String key) {
        String k = key.replace('\\', '/');
        while (k.startsWith("/")) k = k.substring(1);
        return prefix.isEmpty() ? k : prefix + k;
    }

    private static String normalizePrefix(String p) {
        if (p == null || p.isBlank()) return "";
        String s = p.trim().replace('\\', '/');
        while (s.startsWith("/")) s = s.substring(1);
        if (!s.endsWith("/")) s = s + "/";
        return s;
    }
}
