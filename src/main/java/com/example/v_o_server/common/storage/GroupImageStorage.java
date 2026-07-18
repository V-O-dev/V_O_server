package com.example.v_o_server.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 그룹 이미지 저장 이음새(seam). 실제 S3 연동 전까지 {@link NoOpGroupImageStorage}가 대체한다.
 */
public interface GroupImageStorage {

    /**
     * 이미지를 저장하고 저장 결과를 반환한다.
     *
     * @param file 업로드된 이미지
     * @return 저장된 이미지의 URL과 오브젝트 키
     */
    StoredImage store(MultipartFile file);

    /**
     * 저장된 이미지 정보.
     *
     * @param url       공개 접근 URL (미저장 시 null)
     * @param objectKey 스토리지 오브젝트 키 (미저장 시 null)
     */
    record StoredImage(String url, String objectKey) {
    }
}
