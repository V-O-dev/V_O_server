package com.example.v_o_server.domain.group.dto;

import com.example.v_o_server.domain.group.entity.GroupMemberRole;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 그룹 목록 항목")
public record GroupSummaryResponse(
        @Schema(description = "그룹 ID", example = "1")
        Long groupId,

        @Schema(description = "그룹명", example = "우리 가족")
        String name,

        @Schema(description = "그룹 이미지 URL")
        String imageUrl,

        @Schema(description = "테마 코드", example = "FAMILY")
        String themeCode,

        @Schema(description = "활동 멤버 수", example = "4")
        long memberCount,

        @Schema(description = "내 역할", example = "OWNER")
        GroupMemberRole role
) {
    public static GroupSummaryResponse of(PrivateGroup group, long memberCount, GroupMemberRole role) {
        return new GroupSummaryResponse(
                group.getId(),
                group.getName(),
                group.getGroupImageUrl(),
                group.getTheme() == null ? null : group.getTheme().getCode(),
                memberCount,
                role);
    }
}
