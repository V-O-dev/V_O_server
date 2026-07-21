package com.example.v_o_server.domain.auth.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.kakao")
public record KakaoOauthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String adminKey,
        String tokenUri,
        String userInfoUri,
        String unlinkUri
) {
}
