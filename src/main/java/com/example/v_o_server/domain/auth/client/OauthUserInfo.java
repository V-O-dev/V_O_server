package com.example.v_o_server.domain.auth.client;

public record OauthUserInfo(
        String providerUserId,
        String email,
        Boolean emailVerified,
        String displayName,
        String profileImageUrl
) {
}
