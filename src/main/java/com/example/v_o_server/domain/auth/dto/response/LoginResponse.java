package com.example.v_o_server.domain.auth.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {
}
