package com.jdend.erp.common.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * 서버 디스크에 있던 첨부파일을 S3로 한 번 복사한다.
 *
 * <p>{@code app.storage.migrate-on-start=true} 이고 저장소가 s3일 때만 동작한다.
 * 로컬 루트 아래 파일을 같은 상대경로의 키로 올리므로, DB의 file_path는 손대지 않아도
 * S3FileStorage가 절대경로를 키로 변환해 찾아낸다.</p>
 *
 * <p>원본은 지우지 않는다. S3에서 정상적으로 열리는 것을 확인한 뒤 직접 삭제하면 된다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageMigrationRunner implements ApplicationRunner {

    private final StorageProperties properties;
    private final FileStorage storage;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isMigrateOnStart()) return;
        if (!properties.isS3()) {
            log.warn("[StorageMigration] 저장소가 s3가 아니라 건너뜁니다.");
            return;
        }

        Path root = Path.of(properties.getLocalRoot()).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            log.info("[StorageMigration] 옮길 디렉터리가 없습니다: {}", root);
            return;
        }

        log.info("[StorageMigration] 시작 — {} → S3", root);
        int copied = 0, skipped = 0, failed = 0;

        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> files = walk.filter(Files::isRegularFile).toList();
            for (Path f : files) {
                String key = root.relativize(f).toString().replace('\\', '/');
                try {
                    if (storage.exists(key)) {
                        skipped++;
                        continue;
                    }
                    storage.put(key, Files.readAllBytes(f), null);
                    copied++;
                } catch (Exception e) {
                    failed++;
                    log.error("[StorageMigration] 실패 {}: {}", key, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[StorageMigration] 디렉터리 탐색 실패: {}", e.getMessage(), e);
            return;
        }

        log.info("[StorageMigration] 완료 — 복사 {}건, 이미 있음 {}건, 실패 {}건 (원본은 그대로 둡니다)",
                copied, skipped, failed);
    }
}
