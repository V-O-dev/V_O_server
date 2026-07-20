package com.example.v_o_server.common.storage;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * S3 인프라 준비 전 임시로 사용하는 로컬 디스크 저장 구현체.
 * 추후 S3FileStorageService로 교체 예정 (인프라 담당: 민스).
 */
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final String BASE_DIR = "uploads";
    private static final String BASE_URL = "http://localhost:8080/uploads";

    @Override
    public StoredFile upload(MultipartFile file, String directory) {
        try {
            String extension = extractExtension(file.getOriginalFilename());
            String objectKey = directory + "/" + UUID.randomUUID() + extension;

            Path targetPath = Paths.get(BASE_DIR, objectKey);
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);

            String url = BASE_URL + "/" + objectKey;
            return new StoredFile(url, objectKey);
        } catch (IOException e) {
            log.error("파일 업로드 실패", e);
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null) {
            return;
        }
        try {
            Path targetPath = Paths.get(BASE_DIR, objectKey);
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            log.error("파일 삭제 실패", e);
            throw new BusinessException(ErrorCode.IMAGE_DELETE_FAILED);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}