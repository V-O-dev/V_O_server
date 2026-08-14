package com.example.v_o_server.domain.feed.dto;

import com.example.v_o_server.domain.answer.entity.AnswerUploadStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "홈 피드 상단 그룹 탭 항목. GET /api/v1/groups(GroupSummaryResponse)와 "
        + "같은 필드명(name/imageUrl)을 쓴다 — 같은 그룹 요약 정보를 나타내는 두 응답의 이름이 다르면 "
        + "클라이언트가 그룹을 두 가지 형태로 따로 다뤄야 한다.")
public record HomeFeedGroupResponse(
        @Schema(description = "그룹 ID")
        Long groupId,

        @Schema(description = "그룹명")
        String name,

        @Schema(description = "그룹 대표 이미지 URL", nullable = true)
        String imageUrl,

        @Schema(description = "이 그룹의 피드 잠금 해제 여부. false이면 클라이언트에서 이 그룹 영상을 블러 처리")
        boolean unlocked,

        @Schema(description = "조회자의 이 그룹 오늘 답변 상태")
        AnswerUploadStatus viewerAnswerStatus
) {
}
