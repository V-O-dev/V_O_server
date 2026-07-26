package com.example.v_o_server.domain.notification.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.domain.notification.dto.response.NotificationListResponse;
import com.example.v_o_server.domain.notification.dto.response.NotificationReadResponse;
import com.example.v_o_server.domain.notification.service.AppNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class AppNotificationController {

    private final AppNotificationService appNotificationService;

    @Operation(summary = "알림 목록 조회",
            description = "커서 기반 페이지네이션으로 알림을 최신순 조회합니다. cursor는 마지막으로 받은 notificationId.")
    @GetMapping
    public ApiResponse<NotificationListResponse> getNotifications(@AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long cursor) {
        return ApiResponse.success(appNotificationService.getNotifications(userId, cursor));
    }

    @Operation(summary = "알림 읽음 처리", description = "본인이 받은 알림을 읽음 상태로 변경합니다.")
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationReadResponse> markAsRead(@AuthenticationPrincipal Long userId,
            @PathVariable Long notificationId) {
        return ApiResponse.success(appNotificationService.markAsRead(userId, notificationId));
    }
}
