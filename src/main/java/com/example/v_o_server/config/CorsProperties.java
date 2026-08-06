package com.example.v_o_server.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 브라우저(웹 프론트)에서 API를 직접 호출하기 위한 CORS 허용 설정.
 *
 * <p>allowedOrigins는 와일드카드 패턴을 지원한다(예: {@code https://*.vercel.app}).
 * Vercel 프리뷰 배포는 URL이 매번 바뀌므로 패턴 매칭이 필요하다.</p>
 */
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}
