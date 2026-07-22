package com.example.v_o_server.domain.user.dto.request;

import com.example.v_o_server.domain.user.entity.DevicePlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "디바이스 토큰 등록 요청")
public record RegisterDeviceRequest(

        @Schema(description = "디바이스 토큰", example = "fcm-device-token-string")
        @NotBlank(message = "디바이스 토큰이 필요합니다.")
        String deviceToken,

        @Schema(description = "기기 플랫폼", example = "AOS")
        @NotNull(message = "유효하지 않은 플랫폼 값입니다.")
        DevicePlatform platform,

        @Schema(description = "앱 버전", example = "1.0.0")
        String appVersion
) {
}