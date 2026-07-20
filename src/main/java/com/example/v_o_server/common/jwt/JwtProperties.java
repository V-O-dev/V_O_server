package com.example.v_o_server.common.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secretKey,
        long accessTokenValiditySeconds,
        long refreshTokenValiditySeconds
) {
}
