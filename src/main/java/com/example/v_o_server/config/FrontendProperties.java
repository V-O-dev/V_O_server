package com.example.v_o_server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 로그인 완료 후 사용자를 돌려보낼 프론트 기본 주소.
 *
 * <p>로그인 시작 시 {@code redirectUri} 파라미터로 명시하면 그 값이 우선하고,
 * 없을 때만 이 기본값이 쓰인다. (로컬/Vercel 동시 지원을 위해 요청값을 우선한다)</p>
 */
@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(
        String baseUrl,
        /** provider별 콜백 경로 접두사. 최종 주소는 {baseUrl}{oauthCallbackPath}{provider}가 된다. */
        String oauthCallbackPath
) {
}
