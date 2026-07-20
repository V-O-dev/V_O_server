package com.example.v_o_server.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장소 추상화 인터페이스.
 * 현재는 로컬 디스크 구현체(LocalFileStorageService)를 사용하며,
 * 추후 S3 인프라 준비 완료 시 S3FileStorageService로 교체 예정.
 */
public interface FileStorageService {

    /**
     * 파일을 저장하고 접근 가능한 URL과 저장 키(objectKey)를 반환한다.
     */
    StoredFile upload(MultipartFile file, String directory);

    /**
     * 저장된 파일을 삭제한다.
     */
    void delete(String objectKey);

    record StoredFile(String url, String objectKey) {
    }
}