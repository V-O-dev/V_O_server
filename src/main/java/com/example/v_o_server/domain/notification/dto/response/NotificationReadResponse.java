package com.example.v_o_server.domain.notification.dto.response;

import com.example.v_o_server.domain.notification.entity.AppNotification;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 읽음 처리 결과")
public record NotificationReadResponse(

        @Schema(description = "알림 ID", example = "501")
        Long notificationId,

        @Schema(description = "읽음 여부", example = "true")
        boolean isRead
) {

    public static NotificationReadResponse from(AppNotification notification) {
        return new NotificationReadResponse(
                notification.getId(),
                Boolean.TRUE.equals(notification.getIsRead())
        );
    }
}
