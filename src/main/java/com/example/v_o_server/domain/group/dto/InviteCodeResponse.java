package com.example.v_o_server.domain.group.dto;

import com.example.v_o_server.domain.group.entity.GroupInvite;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "초대 코드 발급 응답")
public record InviteCodeResponse(
        @Schema(description = "초대 코드", example = "A3F9K2")
        String code,

        @Schema(description = "초대 링크 (QR에 인코딩되는 값)", example = "https://v-o.app/invites/A3F9K2")
        String inviteUrl,

        @Schema(description = "QR 이미지 경로. API base URL과 합쳐 사용한다.",
                example = "/invites/A3F9K2/qr")
        String qrImageUrl,

        @Schema(description = "만료 시각")
        LocalDateTime expiresAt
) {
    public static InviteCodeResponse from(GroupInvite invite) {
        return new InviteCodeResponse(
                invite.getInviteCode(),
                invite.getInviteUrl(),
                qrPathOf(invite.getInviteCode()),
                invite.getExpiresAt());
    }

    /**
     * QR 이미지 경로.
     *
     * <p>절대 URL은 프록시·도메인 구성에 따라 틀어지므로 상대 경로로 내려준다.
     * 클라이언트는 이미 알고 있는 API base URL과 결합해 사용한다.</p>
     */
    public static String qrPathOf(String code) {
        return "/invites/" + code + "/qr";
    }
}
