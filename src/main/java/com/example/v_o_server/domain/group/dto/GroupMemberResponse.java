package com.example.v_o_server.domain.group.dto;

import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.GroupMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "그룹 멤버 요약")
public record GroupMemberResponse(
        @Schema(description = "멤버 ID (group_members.id)", example = "10")
        Long memberId,

        @Schema(description = "유저 ID", example = "3")
        Long userId,

        @Schema(description = "역할", example = "MEMBER")
        GroupMemberRole role,

        @Schema(description = "가입 시각")
        LocalDateTime joinedAt
) {
    public static GroupMemberResponse from(GroupMember member) {
        return new GroupMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getRole(),
                member.getJoinedAt());
    }
}
