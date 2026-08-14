package com.example.v_o_server.domain.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "홈 피드 영상 항목. 그룹 피드(GET /groups/{groupId}/feed) 아이템에 소속 그룹 정보를 더한 형태다")
public record HomeFeedItemResponse(
        @Schema(description = "이 영상이 속한 그룹 ID")
        Long groupId,

        @Schema(description = "이 영상이 속한 그룹명")
        String groupName,

        @Schema(description = "이 영상이 속한 그룹의 피드 잠금 해제 여부. false여도 video 필드 구성은 동일하며, "
                + "블러 처리는 클라이언트 책임이다(그룹 피드와 동일한 규약).")
        boolean unlocked,

        @Schema(description = "영상 상세")
        FeedItemResponse video
) {
}
