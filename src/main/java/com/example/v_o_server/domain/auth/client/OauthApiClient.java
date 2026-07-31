package com.example.v_o_server.domain.auth.client;

import com.example.v_o_server.domain.auth.entity.OauthProvider;

/**
 * provider별(Google/Kakao) OAuth 연동을 추상화한다.
 * {@link OauthProvider}에 따라 알맞은 구현체가 선택된다.
 */
public interface OauthApiClient {

    OauthProvider getProvider();

    /**
     * 인가 코드를 provider의 access token으로 교환한다.
     *
     * @param redirectUri 인가 코드를 발급받을 때 사용한 값. provider가 동일 여부를 검증하므로
     *                    프론트가 실제로 사용한 값을 그대로 넘겨야 한다.
     *                    null이면 서버 설정값(oauth.{provider}.redirect-uri)을 사용한다.
     * @throws com.example.v_o_server.common.exception.BusinessException
     *         인가 코드가 잘못된 경우 OAUTH_INVALID_CODE, provider 통신 실패 시 OAUTH_PROVIDER_ERROR
     */
    OauthTokenResult exchangeToken(String authorizationCode, String redirectUri);

    /**
     * @throws com.example.v_o_server.common.exception.BusinessException provider 통신 실패 시 OAUTH_PROVIDER_ERROR
     */
    OauthUserInfo fetchUserInfo(String providerAccessToken);

    /**
     * 계정 연결 해제를 시도한다 (best-effort). 실패해도 예외를 던지지 않고 로그만 남긴다.
     *
     * @param providerAccessToken 로그인 시점에만 존재하는 값이라 탈퇴 시점엔 보통 null이다.
     */
    void unlink(String providerUserId, String providerAccessToken);
}
