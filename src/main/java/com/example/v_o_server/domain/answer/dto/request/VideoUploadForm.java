package com.example.v_o_server.domain.answer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

/**
 * 영상 업로드 multipart 폼.
 *
 * <p>{@link VideoUploadMetadataRequest} 와 같은 필드에 영상 파일을 더한 폼 바인딩 전용 DTO.
 * 파일을 {@code @RequestPart} 로 분리하고 나머지를 별도 {@code @ModelAttribute} 로 받으면
 * springdoc 이 그 나머지 필드들을 개별 입력칸이 아닌 object 파트 하나로 뭉뚱그려 문서화해
 * Swagger 에서 JSON 입력창으로 뜨므로, 파일을 폼 객체 안에 두어 함께 문서화되게 한다.</p>
 */
@Schema(description = "영상 업로드 폼")
public record VideoUploadForm(
        @Schema(description = "답변 영상 파일", type = "string", format = "binary")
        MultipartFile video,

        @NotNull
        @Schema(description = "그룹 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        Long groupId,

        @NotNull
        @Schema(description = "오늘의 질문 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        Long questionId,

        @Positive
        @Max(value = 15_000, message = "영상 길이는 15초 이하여야 합니다.")
        @Schema(description = "영상 길이(ms)", nullable = true)
        Integer durationMs,

        @Positive
        @Schema(description = "영상 가로 크기(px)", nullable = true)
        Integer width,

        @Positive
        @Schema(description = "영상 세로 크기(px)", nullable = true)
        Integer height,

        @Pattern(regexp = "FRONT|BACK", message = "카메라 방향은 FRONT 또는 BACK이어야 합니다.")
        @Schema(description = "카메라 방향", allowableValues = {"FRONT", "BACK"}, nullable = true)
        String cameraFacing,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Schema(description = "클라이언트 촬영 시각", nullable = true)
        LocalDateTime capturedAt
) {
    public VideoUploadMetadataRequest toMetadata() {
        return new VideoUploadMetadataRequest(groupId, questionId, durationMs, width, height, cameraFacing, capturedAt);
    }
}
