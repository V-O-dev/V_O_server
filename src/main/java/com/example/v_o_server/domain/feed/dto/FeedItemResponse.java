package com.example.v_o_server.domain.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "피드 영상 항목")
public record FeedItemResponse(
        @Schema(description = "영상 ID")
        Long videoId,

        @Schema(description = "작성자 사용자 ID")
        Long userId,

        @Schema(description = "작성자가 직접 설정한 전역 닉네임", nullable = true)
        String nickname,

        @Schema(description = "화면에 표시할 이름. 내가 이 그룹에서 작성자에게 지정한 호칭이 있으면 그 호칭, 없으면 닉네임",
                example = "엄마", nullable = true)
        String displayName,

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

        @Schema(description = "좋아요 수")
        long reactionCount,

        @Schema(description = "조회자의 좋아요 여부")
        boolean reactedByMe,

        @Schema(description = "삭제되지 않은 댓글 수")
        long commentCount,

        @Schema(description = "촬영 시각", nullable = true)
        LocalDateTime capturedAt,

        @Schema(description = "업로드 시각", nullable = true)
        LocalDateTime uploadedAt
) {
}
