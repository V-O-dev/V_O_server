package com.example.v_o_server.domain.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "알림 목록 (최신순)")
public record NotificationListResponse(

        @Schema(description = "알림 목록. 없으면 빈 배열")
        List<NotificationResponse> notifications,

        @Schema(description = "다음 페이지 존재 여부", example = "false")
        boolean hasNext
) {
}
