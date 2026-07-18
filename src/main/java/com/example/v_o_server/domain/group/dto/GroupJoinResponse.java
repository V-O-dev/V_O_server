package com.example.v_o_server.domain.group.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "그룹 가입 응답")
public record GroupJoinResponse(
        @Schema(description = "가입한 그룹 ID", example = "1")
        Long groupId,

        @Schema(description = "그룹명", example = "우리 가족")
        String groupName
) {
}
