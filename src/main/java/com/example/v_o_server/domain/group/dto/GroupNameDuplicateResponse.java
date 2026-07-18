package com.example.v_o_server.domain.group.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "그룹명 중복 확인 응답")
public record GroupNameDuplicateResponse(
        @Schema(description = "중복 여부 (true면 사용 불가)", example = "false")
        boolean duplicated
) {
}
