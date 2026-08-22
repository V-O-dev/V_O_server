package com.example.v_o_server.domain.answer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "답변 영상 응답")
public record VideoResponse(
        @Schema(description = "답변 영상 ID", example = "1")
        Long videoId,

        @Schema(description = "그룹 ID", example = "1")
        Long groupId,

        @Schema(description = "질문 ID", example = "3")
        Long questionId,

        @Schema(description = "영상 URL")
        String videoUrl,

        @Schema(description = "썸네일 URL")
        String thumbnailUrl,

        @Schema(description = "영상 길이(ms)", example = "10000")
        Integer durationMs,

        @Schema(description = "업로드 일시 (KST 오프셋 포함)", example = "2026-08-22T12:00:00+09:00")
        OffsetDateTime uploadedAt
) {
}
