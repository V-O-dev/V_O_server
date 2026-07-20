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
     * @throws com.example.v_o_server.common.exception.BusinessException
     *         인가 코드가 잘못된 경우 OAUTH_INVALID_CODE, provider 통신 실패 시 OAUTH_PROVIDER_ERROR
     */
    OauthTokenResult exchangeToken(String authorizationCode);

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
