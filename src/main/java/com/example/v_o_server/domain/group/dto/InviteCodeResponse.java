package com.example.v_o_server.domain.group.dto;

import com.example.v_o_server.domain.group.entity.GroupInvite;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "초대 코드 발급 응답")
public record InviteCodeResponse(
        @Schema(description = "초대 코드", example = "A3F9K2")
        String code,

        @Schema(description = "초대 링크 (QR 생성용)", example = "https://v-o.app/invites/A3F9K2")
        String inviteUrl,

        @Schema(description = "만료 시각")
        LocalDateTime expiresAt
) {
    public static InviteCodeResponse from(GroupInvite invite) {
        return new InviteCodeResponse(invite.getInviteCode(), invite.getInviteUrl(), invite.getExpiresAt());
    }
}
