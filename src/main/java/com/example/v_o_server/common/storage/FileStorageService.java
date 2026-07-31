package com.example.v_o_server.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장소 추상화 인터페이스.
 * storage.type 설정값에 따라 LocalFileStorageService 또는 S3FileStorageService가 선택된다.
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