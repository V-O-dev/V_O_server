package com.example.v_o_server.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * accessToken(JWT) 발급/파싱을 전담한다.
 * refreshToken은 opaque 토큰(해시 저장)이라 여기서 다루지 않는다.
 */
@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenValiditySeconds;

    public JwtProvider(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(jwtProperties.secretKey().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValiditySeconds = jwtProperties.accessTokenValiditySeconds();
    }

    public String createAccessToken(Long userId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenValiditySeconds);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /** 유효하지 않거나 만료된 토큰이면 {@link JwtException}을 던진다. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }

    /** 토큰의 남은 유효시간(초). 이미 만료됐다면 0. */
    public long getRemainingSeconds(Claims claims) {
        long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
        return Math.max(remainingMillis / 1000, 0);
    }
}
