package com.example.v_o_server.common.storage;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 로컬 개발 환경에서 사용하는 디스크 저장 구현체.
 * storage.type=local(기본값)일 때 활성화되며, storage.type=s3이면 S3FileStorageService가 대신 사용된다.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private static final String BASE_DIR = "uploads";
    private static final String BASE_URL = "http://localhost:8080/uploads";
    private static final Pattern SAFE_EXTENSION = Pattern.compile("\\.[a-z0-9]{1,10}");

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
        String normalizedFilename = originalFilename.replace('\\', '/');
        String filename = normalizedFilename.substring(normalizedFilename.lastIndexOf('/') + 1);
        String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        return SAFE_EXTENSION.matcher(extension).matches() ? extension : "";
    }
}
