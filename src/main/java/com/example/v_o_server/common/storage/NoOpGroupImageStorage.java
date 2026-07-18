package com.example.v_o_server.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * S3 연동 전 임시 구현. 이미지를 실제로 저장하지 않고 빈 결과를 반환한다.
 */
@Slf4j
@Component
public class NoOpGroupImageStorage implements GroupImageStorage {

    @Override
    public StoredImage store(MultipartFile file) {
        log.info("이미지 스토리지 미연동 - 저장을 건너뜁니다. filename={}, size={}",
                file.getOriginalFilename(), file.getSize());
        return new StoredImage(null, null);
    }
}
