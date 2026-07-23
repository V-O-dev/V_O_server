package com.example.v_o_server.domain.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "피드 영상 항목")
public record FeedItemResponse(
        @Schema(description = "영상 ID")
        Long videoId,

        @Schema(description = "작성자 사용자 ID")
        Long userId,

        @Schema(description = "작성자 닉네임", nullable = true)
        String nickname,

        @Schema(description = "작성자 프로필 이미지 URL", nullable = true)
        String profileImageUrl,

        @Schema(description = "질문 ID")
        Long questionId,

        @Schema(description = "질문 내용")
        String questionContent,

        @Schema(description = "영상 URL")
        String videoUrl,

        @Schema(description = "썸네일 URL", nullable = true)
        String thumbnailUrl,

        @Schema(description = "영상 길이(ms)", nullable = true)
        Integer durationMs,

        @Schema(description = "촬영 시각", nullable = true)
        LocalDateTime capturedAt,

        @Schema(description = "업로드 시각", nullable = true)
        LocalDateTime uploadedAt
) {
}
