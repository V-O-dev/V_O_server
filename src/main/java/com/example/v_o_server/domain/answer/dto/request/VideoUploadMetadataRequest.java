package com.example.v_o_server.domain.answer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "영상 업로드 폼 메타데이터")
public record VideoUploadMetadataRequest(
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
}
