package com.example.v_o_server.domain.auth.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.naver")
public record NaverOauthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String authorizeUri,
        String tokenUri,
        String userInfoUri
) {
}
