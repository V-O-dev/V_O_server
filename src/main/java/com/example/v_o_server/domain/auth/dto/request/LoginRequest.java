package com.example.v_o_server.domain.auth.dto.request;

import com.example.v_o_server.domain.auth.entity.OauthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @Schema(description = "OAuth provider", example = "KAKAO")
        @NotNull OauthProvider provider,

        @Schema(description = "provider로부터 발급받은 인가 코드")
        @NotBlank String authorizationCode,

        /*
         * 인가 코드를 발급받을 때 사용한 redirect_uri.
         * provider는 토큰 교환 시 authorize 단계에서 쓴 값과 동일한지 검증하므로,
         * 프론트 실행 환경(localhost / Vercel 프리뷰 / 프로덕션)마다 값이 달라진다.
         * 서버 설정에 하나로 고정하면 환경이 늘어날 때마다 재배포해야 해서 요청으로 받는다.
         * 생략하면 서버 설정값(oauth.{provider}.redirect-uri)으로 폴백한다.
         */
        @Schema(description = "인가 코드 발급에 사용한 redirect_uri (생략 시 서버 기본값 사용)",
                example = "http://localhost:3000/oauth/callback/kakao")
        String redirectUri
) {
}
