package com.example.v_o_server.domain.notification.dto.response;

import com.example.v_o_server.domain.notification.entity.AppNotification;
import com.example.v_o_server.domain.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "알림 단건")
public record NotificationResponse(

        @Schema(description = "알림 ID", example = "501")
        Long notificationId,

        @Schema(description = "알림 유형 (프론트 아이콘 분기용)", example = "COMMENT")
        NotificationType type,

        @Schema(description = "화면에 그대로 표시할 완성된 문장",
                example = "아빠님이 댓글을 남겼습니다: \"하허허 어쩌구 저쩌구\"")
        String content,

        @Schema(description = "관련 그룹명", example = "가족 그룹")
        String groupName,

        @Schema(description = "이동할 영상 ID. DAILY_QUESTION 타입은 null", example = "87")
        Long relatedVideoId,

        @Schema(description = "읽음 여부", example = "false")
        boolean isRead,

        @Schema(description = "알림 생성 시각")
        LocalDateTime createdAt
) {

    public static NotificationResponse from(AppNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getNotificationType(),
                notification.getBody(),
                notification.getGroup() == null ? null : notification.getGroup().getName(),
                notification.getVideo() == null ? null : notification.getVideo().getId(),
                Boolean.TRUE.equals(notification.getIsRead()),
                notification.getCreatedAt()
        );
    }
}
