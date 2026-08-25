package com.jdend.erp.common.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 서버 디스크에 저장한다.
 *
 * <p>예전에 저장된 레코드는 file_path에 절대경로가 들어 있으므로,
 * 절대경로가 들어오면 루트를 붙이지 않고 그대로 사용한다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class LocalFileStorage implements FileStorage {

    private final String root;

    @Override
    public void put(String key, byte[] content, String contentType) {
        Path target = resolve(key);
        try {
            Path parent = target.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(target, content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패(" + target + "): " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] get(String key) {
        Path target = resolve(key);
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new RuntimeException("파일을 읽을 수 없습니다(" + target + "): " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolve(key));
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("[Storage] 파일 삭제 실패 {}: {}", key, e.getMessage());
        }
    }

    @Override
    public String describe(String key) {
        return resolve(key).toString();
    }

    private Path resolve(String key) {
        Path p = Path.of(key);
        if (p.isAbsolute()) return p.normalize();          // 과거 절대경로 레코드 호환
        return Path.of(root).toAbsolutePath().resolve(key).normalize();
    }
}
