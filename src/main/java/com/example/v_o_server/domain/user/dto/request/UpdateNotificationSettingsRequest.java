package com.example.v_o_server.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "알림 설정 변경 요청")
public record UpdateNotificationSettingsRequest(

        @Schema(description = "오늘의 질문 알림 수신 여부", example = "true")
        @NotNull(message = "오늘의 질문 알림 여부는 필수입니다.")
        Boolean questionNotification,

        @Schema(description = "댓글·좋아요 알림 수신 여부", example = "false")
        @NotNull(message = "댓글·좋아요 알림 여부는 필수입니다.")
        Boolean interactionNotification
) {
}