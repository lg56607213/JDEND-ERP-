package com.jdend.erp.common.storage;

/**
 * 첨부파일 저장소.
 *
 * <p>키는 저장소 루트 기준 상대 경로다. 예) {@code accounts/documents/계약서/2026-08/uuid.pdf}
 * 로컬 저장소는 {app.storage.local-root}/{키}에, S3 저장소는 같은 키로 객체를 만든다.
 * 두 구현의 키 공간이 동일하므로 설정만 바꾸면 서로 전환된다.</p>
 */
public interface FileStorage {

    /** 파일을 저장한다. 같은 키가 있으면 덮어쓴다. */
    void put(String key, byte[] content, String contentType);

    /** 파일 내용을 읽는다. 없으면 예외. */
    byte[] get(String key);

    boolean exists(String key);

    /** 없으면 조용히 넘어간다. */
    void delete(String key);

    /** 로그/진단용 위치 문자열 */
    String describe(String key);
}
