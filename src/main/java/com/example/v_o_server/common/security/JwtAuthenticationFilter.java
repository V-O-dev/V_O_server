package com.example.v_o_server.common.security;

import com.example.v_o_server.common.jwt.JwtProvider;
import com.example.v_o_server.common.jwt.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization: Bearer {accessToken} 헤더를 검증해 SecurityContext에 인증 정보(principal=userId)를 심는다.
 * 토큰이 없거나 유효하지 않으면 그냥 통과시키고, 이후 인가 규칙에서 401/403으로 처리된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final AccountStatusChecker accountStatusChecker;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null) {
            try {
                Claims claims = jwtProvider.parse(token);
                String jti = jwtProvider.getJti(claims);
                if (!tokenBlacklistService.isBlacklisted(jti)) {
                    Long userId = jwtProvider.getUserId(claims);
                    // 토큰이 유효해도 탈퇴한 계정이면 인증하지 않는다.
                    // (탈퇴 시 블랙리스트에 넣지 못한 다른 기기의 accessToken을 여기서 막는다)
                    if (accountStatusChecker.isActive(userId)) {
                        var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        log.debug("사용할 수 없는 계정의 토큰입니다. userId={}", userId);
                    }
                }
            } catch (JwtException | NumberFormatException e) {
                log.debug("Invalid access token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        return null;
    }
}
