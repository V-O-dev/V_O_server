package com.example.v_o_server.common.storage;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * AWS S3 저장 구현체. storage.type=s3일 때 활성화된다.
 * 버킷은 퍼블릭 읽기로 열려있다고 가정하며(objectKey는 추측 불가능한 UUID),
 * 자격 증명은 EC2 인스턴스에 연결된 IAM 역할을 통해 자동으로 확인한다(액세스 키를 코드/설정에 직접 넣지 않음).
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final String bucket;

    public S3FileStorageService(@Value("${storage.s3.region}") String region,
            @Value("${storage.s3.bucket}") String bucket) {
        this.s3Client = S3Client.builder().region(Region.of(region)).build();
        this.bucket = bucket;
    }

    @Override
    public StoredFile upload(MultipartFile file, String directory) {
        String extension = extractExtension(file.getOriginalFilename());
        String objectKey = directory + "/" + UUID.randomUUID() + extension;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | SdkException e) {
            log.error("S3 파일 업로드 실패", e);
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }

        String url = s3Client.utilities()
                .getUrl(GetUrlRequest.builder().bucket(bucket).key(objectKey).build())
                .toString();
        return new StoredFile(url, objectKey);
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
        } catch (SdkException e) {
            log.error("S3 파일 삭제 실패", e);
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
