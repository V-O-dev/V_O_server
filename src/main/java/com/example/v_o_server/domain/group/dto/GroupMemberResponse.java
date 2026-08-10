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
        LocalDateTime joinedAt,

        @Schema(description = "대상이 직접 설정한 전역 닉네임. 온보딩 전이면 null",
                example = "홍길동", nullable = true)
        String nickname,

        @Schema(description = "대상의 프로필 이미지 URL", nullable = true)
        String profileImageUrl,

        @Schema(description = "내가 이 멤버에게 지정한 호칭. 지정하지 않았으면 null (나에게만 보인다)",
                example = "엄마", nullable = true)
        String alias,

        @Schema(description = "화면에 그대로 표시할 이름. 호칭이 있으면 호칭, 없으면 닉네임",
                example = "엄마", nullable = true)
        String displayName,

        @Schema(description = "호출자 본인 여부", example = "false")
        boolean isMe
) {

    /** 호칭·프로필을 붙이지 않은 최소 형태. 조립은 {@code GroupMemberViewAssembler}가 담당한다. */
    public static GroupMemberResponse of(GroupMember member, String nickname, String profileImageUrl,
                                         String alias, String displayName, boolean isMe) {
        return new GroupMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getRole(),
                member.getJoinedAt(),
                nickname,
                profileImageUrl,
                alias,
                displayName,
                isMe);
    }
}
