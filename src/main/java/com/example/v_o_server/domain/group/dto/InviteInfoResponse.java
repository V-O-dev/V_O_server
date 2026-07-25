package com.example.v_o_server.domain.group.dto;

import com.example.v_o_server.domain.group.entity.PrivateGroup;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "초대 정보 조회 응답")
public record InviteInfoResponse(
        @Schema(description = "그룹 ID", example = "1")
        Long groupId,

        @Schema(description = "그룹명", example = "우리 가족")
        String groupName,

        @Schema(description = "테마 코드", example = "FAMILY")
        String themeCode,

        @Schema(description = "활동 멤버 수", example = "4")
        long memberCount
) {
    public static InviteInfoResponse of(PrivateGroup group, long memberCount) {
        return new InviteInfoResponse(
                group.getId(),
                group.getName(),
                group.getTheme() == null ? null : group.getTheme().getCode(),
                memberCount);
    }
}
