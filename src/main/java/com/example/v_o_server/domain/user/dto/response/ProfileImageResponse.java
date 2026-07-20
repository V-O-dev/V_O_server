package com.example.v_o_server.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로필 이미지 응답")
public record ProfileImageResponse(
        @Schema(description = "프로필 이미지 URL (삭제 시 null)", example = "https://.../profile.jpg")
        String profileImageUrl
) {
}