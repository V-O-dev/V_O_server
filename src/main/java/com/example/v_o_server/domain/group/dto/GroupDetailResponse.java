package com.example.v_o_server.domain.group.dto;

import com.example.v_o_server.domain.group.entity.PrivateGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "그룹 상세 응답")
public record GroupDetailResponse(
        @Schema(description = "그룹 ID", example = "1")
        Long groupId,

        @Schema(description = "그룹명", example = "우리 가족")
        String name,

        @Schema(description = "그룹 이미지 URL")
        String imageUrl,

        @Schema(description = "테마 코드", example = "FAMILY")
        String themeCode,

        @Schema(description = "방장 유저 ID", example = "1")
        Long ownerUserId,

        @Schema(description = "질문 알림 시작 시간", example = "20:00")
        LocalTime notificationStartTime,

        @Schema(description = "질문 알림 종료 시간", example = "21:00")
        LocalTime notificationEndTime,

        @Schema(description = "타임존", example = "Asia/Seoul")
        String timezone,

        @Schema(description = "최대 멤버 수", example = "15")
        Integer maxMembers,

        @Schema(description = "활동 멤버 수", example = "4")
        long memberCount,

        @Schema(description = "멤버 목록")
        List<GroupMemberResponse> members
) {
    public static GroupDetailResponse of(PrivateGroup group, List<GroupMemberResponse> members) {
        return new GroupDetailResponse(
                group.getId(),
                group.getName(),
                group.getGroupImageUrl(),
                group.getTheme() == null ? null : group.getTheme().getCode(),
                group.getOwner() == null ? null : group.getOwner().getId(),
                group.getNotificationStartTime(),
                group.getNotificationEndTime(),
                group.getTimezone(),
                group.getMaxMembers(),
                members.size(),
                members);
    }
}
