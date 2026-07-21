package com.example.v_o_server.domain.auth.client;

/**
 * provider 토큰 교환 결과. accessToken은 사용자 정보 조회에 즉시 사용되고 저장되지 않는다.
 */
public record OauthTokenResult(
        String accessToken,
        String refreshToken,
        Long expiresInSeconds
) {
}
