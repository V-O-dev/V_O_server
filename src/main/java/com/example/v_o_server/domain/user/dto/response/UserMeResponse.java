package com.example.v_o_server.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 정보 조회 응답")
public record UserMeResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "닉네임", example = "나타샤")
        String nickname,

        @Schema(description = "프로필 이미지 URL", example = "https://.../profile.jpg")
        String profileImageUrl,

        @Schema(description = "OAuth 로그인 제공자", example = "KAKAO")
        String provider,

        @Schema(description = "오늘의 질문 알림 수신 여부", example = "true")
        Boolean questionNotification,

        @Schema(description = "댓글·좋아요 알림 수신 여부", example = "true")
        Boolean interactionNotification
) {
}