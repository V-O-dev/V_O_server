package com.example.v_o_server.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "디바이스 토큰 해제 요청")
public record UnregisterDeviceRequest(

        @Schema(description = "디바이스 토큰", example = "fcm-device-token-string")
        @NotBlank(message = "디바이스 토큰이 필요합니다.")
        String deviceToken
) {
}