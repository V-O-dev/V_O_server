package com.example.v_o_server.domain.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Schema(description = "피드 영상 항목")
public record FeedItemResponse(
        @Schema(description = "영상 ID")
        Long videoId,

        @Schema(description = "작성자 사용자 ID")
        Long userId,

        @Schema(description = "작성자의 그룹 멤버 ID (group_members.id). 호칭 편집 화면으로 이동할 때 쓴다. "
                + "내 영상이거나 작성 후 그룹을 나간 사람이면 null이며, 이 경우 호칭을 지정할 수 없다.",
                example = "10", nullable = true)
        Long memberId,

        @Schema(description = "내가 올린 영상인지 여부. true면 호칭 편집 화면으로 진입시키지 않는다 — "
                + "자기 자신에게는 호칭을 지정할 수 없다. "
                + "memberId가 null인 것으로는 판단할 수 없다. 그룹을 나간 작성자도 null이기 때문이다.",
                example = "false")
        boolean isMe,

        @Schema(description = "작성자가 직접 설정한 전역 닉네임", nullable = true)
        String nickname,

        @Schema(description = "내가 이 그룹에서 작성자에게 지정한 호칭. 지정하지 않았으면 null. "
                + "이름 편집 화면의 입력 기본값으로 쓴다.",
                example = "엄마", nullable = true)
        String alias,

        @Schema(description = "화면에 표시할 이름. 호칭이 있으면 호칭, 없으면 닉네임",
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

        @Schema(
                description = "업로드 시각 (KST 오프셋 포함)",
                example = "2026-08-22T12:00:00+09:00",
                nullable = true
        )
        OffsetDateTime uploadedAt
) {
}
