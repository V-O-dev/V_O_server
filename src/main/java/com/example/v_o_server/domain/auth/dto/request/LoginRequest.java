package com.example.v_o_server.domain.auth.dto.request;

import com.example.v_o_server.domain.auth.entity.OauthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @Schema(description = "OAuth provider", example = "KAKAO")
        @NotNull OauthProvider provider,

        @Schema(description = "provider로부터 발급받은 인가 코드")
        @NotBlank String authorizationCode
) {
}
