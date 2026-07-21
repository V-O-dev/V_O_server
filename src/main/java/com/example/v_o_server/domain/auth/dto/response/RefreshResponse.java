package com.example.v_o_server.domain.auth.dto.response;

public record RefreshResponse(
        String accessToken,
        String refreshToken
) {
}
