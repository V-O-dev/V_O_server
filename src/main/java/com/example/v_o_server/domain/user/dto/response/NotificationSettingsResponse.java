package com.example.v_o_server.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 설정 응답")
public record NotificationSettingsResponse(
        @Schema(description = "오늘의 질문 알림 수신 여부", example = "true")
        Boolean questionNotification,

        @Schema(description = "댓글·좋아요 알림 수신 여부", example = "false")
        Boolean interactionNotification
) {
}