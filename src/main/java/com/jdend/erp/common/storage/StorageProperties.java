package com.jdend.erp.common.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** local | s3 */
    private String type = "local";

    /** 로컬 저장소 루트 (키가 이 아래에 놓인다) */
    private String localRoot = "uploads";

    private S3 s3 = new S3();

    /** true면 기동 시 로컬 파일을 S3로 한 번 복사한다. 이전이 끝나면 false로 되돌린다. */
    private boolean migrateOnStart = false;

    @Getter
    @Setter
    public static class S3 {
        private String bucket;
        private String region = "ap-northeast-2";
        /** 버킷 안에서 사용할 접두사 (비워두면 키를 그대로 사용) */
        private String prefix = "";
    }

    public boolean isS3() {
        return "s3".equalsIgnoreCase(type);
    }
}
