package com.example.v_o_server.domain.group.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "그룹 생성 응답")
public record GroupCreateResponse(
        @Schema(description = "생성된 그룹 ID", example = "1")
        Long groupId,

        @Schema(description = "그룹 상세")
        GroupDetailResponse group
) {
}
