package com.example.v_o_server.domain.auth.client;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.auth.client.dto.GoogleTokenResponse;
import com.example.v_o_server.domain.auth.client.dto.GoogleUserInfoResponse;
import com.example.v_o_server.domain.auth.entity.OauthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Google OAuth 연동.
 * <p>공식 문서: <a href="https://developers.google.com/identity/protocols/oauth2/web-server">Google OAuth2</a>
 * TODO: 실제 client-id/secret 발급 후 토큰 교환 및 사용자 정보 응답 스펙이 최신인지 확인 필요</p>
 */
@Slf4j
@Component
public class GoogleOauthApiClient implements OauthApiClient {

    private final GoogleOauthProperties properties;
    private final RestClient restClient = RestClient.create();

    public GoogleOauthApiClient(GoogleOauthProperties properties) {
        this.properties = properties;
    }

    @Override
    public OauthProvider getProvider() {
        return OauthProvider.GOOGLE;
    }

    @Override
    public String buildAuthorizeUrl(String redirectUri, String state) {
        return UriComponentsBuilder.fromUriString(properties.authorizeUri())
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                // 구글은 scope를 명시해야 이메일/프로필을 내려준다
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .encode()
                .toUriString();
    }

    @Override
    public OauthTokenResult exchangeToken(String authorizationCode, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", authorizationCode);
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("redirect_uri", resolveRedirectUri(redirectUri));
        form.add("grant_type", "authorization_code");

        try {
            GoogleTokenResponse response = restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GoogleTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "Google 토큰 응답이 비어있습니다.");
            }
            return new OauthTokenResult(response.accessToken(), response.refreshToken(), response.expiresIn());
        } catch (RestClientResponseException e) {
            throw mapTokenExchangeError(e);
        } catch (RestClientException e) {
            log.error("Google 토큰 교환 통신 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "Google 서버와 통신할 수 없습니다.");
        }
    }

    @Override
    public OauthUserInfo fetchUserInfo(String providerAccessToken) {
        try {
            GoogleUserInfoResponse response = restClient.get()
                    .uri(properties.userInfoUri())
                    .header("Authorization", "Bearer " + providerAccessToken)
                    .retrieve()
                    .body(GoogleUserInfoResponse.class);

            if (response == null || response.sub() == null) {
                throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "Google 사용자 정보 응답이 비어있습니다.");
            }
            return new OauthUserInfo(response.sub(), response.email(), response.emailVerified(),
                    response.name(), response.picture());
        } catch (RestClientException e) {
            log.error("Google 사용자 정보 조회 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "Google 사용자 정보를 가져올 수 없습니다.");
        }
    }

    @Override
    public void unlink(String providerUserId, String providerAccessToken) {
        // 구조적 한계: 탈퇴 시점엔 로그인 때 받았던 access/refresh token을 저장해두지 않아 호출할 토큰이 없다.
        // (entity 스펙에 provider 토큰 저장 컬럼이 없음) 저장 컬럼이 추가되기 전까지는 항상 스킵된다.
        if (providerAccessToken == null) {
            log.error("[{}] Google unlink 실패: 저장된 provider token이 없어 revoke 호출을 시도할 수 없습니다. providerUserId={}",
                    ErrorCode.OAUTH_UNLINK_FAILED.getCode(), providerUserId);
            return;
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("token", providerAccessToken);
            restClient.post()
                    .uri(properties.revokeUri())
                    .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("[{}] Google unlink 호출 실패. providerUserId={}", ErrorCode.OAUTH_UNLINK_FAILED.getCode(),
                    providerUserId, e);
        }
    }

    /** 프론트가 실제로 사용한 redirect_uri를 우선하고, 없으면 서버 설정값으로 폴백한다. */
    private String resolveRedirectUri(String requested) {
        return (requested != null && !requested.isBlank()) ? requested : properties.redirectUri();
    }

    private BusinessException mapTokenExchangeError(RestClientResponseException e) {
        HttpStatusCode status = e.getStatusCode();
        if (status.is4xxClientError()) {
            log.warn("Google 인가 코드 검증 실패: {}", e.getResponseBodyAsString());
            return new BusinessException(ErrorCode.OAUTH_INVALID_CODE);
        }
        log.error("Google 토큰 교환 서버 오류: {}", e.getResponseBodyAsString(), e);
        return new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
    }
}
