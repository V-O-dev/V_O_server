package com.example.v_o_server.domain.auth.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.config.CorsProperties;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;

/**
 * 로그인 완료 후 사용자를 돌려보낼 프론트 주소를 검증한다.
 *
 * <p>이 값을 검증 없이 신뢰하면 오픈 리다이렉트 취약점이 된다.
 * 공격자가 {@code ?redirectUri=https://공격자.com/}으로 로그인 링크를 만들어 배포하면
 * 정상 로그인 후 토큰이 공격자 주소로 그대로 실려 가기 때문이다.</p>
 *
 * <p>허용 목록은 CORS 설정({@code cors.allowed-origins})을 그대로 재사용한다.
 * "브라우저에서 우리 API를 부를 수 있는 출처"와 "로그인 후 돌아갈 수 있는 출처"가 동일해야 하므로
 * 목록을 이중 관리하지 않는다.</p>
 */
@Component
public class FrontendRedirectValidator {

    private final CorsConfiguration corsConfiguration;

    public FrontendRedirectValidator(CorsProperties corsProperties) {
        this.corsConfiguration = new CorsConfiguration();
        this.corsConfiguration.setAllowedOriginPatterns(corsProperties.allowedOrigins());
    }

    /**
     * @throws BusinessException 허용되지 않은 출처면 INVALID_INPUT_VALUE
     */
    public void validate(String redirectUri) {
        String origin = extractOrigin(redirectUri);
        if (corsConfiguration.checkOrigin(origin) == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "허용되지 않은 리다이렉트 주소입니다: " + redirectUri);
        }
    }

    private String extractOrigin(String redirectUri) {
        try {
            URI uri = new URI(redirectUri);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "리다이렉트 주소는 절대 URL이어야 합니다: " + redirectUri);
            }
            String origin = uri.getScheme() + "://" + uri.getHost();
            return uri.getPort() == -1 ? origin : origin + ":" + uri.getPort();
        } catch (URISyntaxException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "리다이렉트 주소 형식이 올바르지 않습니다: " + redirectUri);
        }
    }
}
