package com.example.v_o_server.domain.auth.client;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.auth.client.dto.KakaoTokenResponse;
import com.example.v_o_server.domain.auth.client.dto.KakaoUserInfoResponse;
import com.example.v_o_server.domain.auth.entity.OauthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Kakao OAuth 연동.
 * <p>공식 문서: <a href="https://developers.kakao.com/docs/latest/ko/kakaologin/common">카카오 로그인</a>
 * TODO: 실제 REST API 키/Admin 키 발급 후 토큰 교환 및 사용자 정보 응답 스펙이 최신인지 확인 필요</p>
 */
@Slf4j
@Component
public class KakaoOauthApiClient implements OauthApiClient {

    private final KakaoOauthProperties properties;
    private final RestClient restClient = RestClient.create();

    public KakaoOauthApiClient(KakaoOauthProperties properties) {
        this.properties = properties;
    }

    @Override
    public OauthProvider getProvider() {
        return OauthProvider.KAKAO;
    }

    @Override
    public String buildAuthorizeUrl(String redirectUri, String state) {
        return UriComponentsBuilder.fromUriString(properties.authorizeUri())
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .encode()
                .toUriString();
    }

    @Override
    public OauthTokenResult exchangeToken(String authorizationCode, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.clientId());
        if (properties.clientSecret() != null && !properties.clientSecret().isBlank()) {
            form.add("client_secret", properties.clientSecret());
        }
        form.add("redirect_uri", resolveRedirectUri(redirectUri));
        form.add("code", authorizationCode);

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "Kakao 토큰 응답이 비어있습니다.");
            }
            return new OauthTokenResult(response.accessToken(), response.refreshToken(), response.expiresIn());
        } catch (RestClientResponseException e) {
            throw mapTokenExchangeError(e);
        } catch (RestClientException e) {
            log.error("Kakao 토큰 교환 통신 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "Kakao 서버와 통신할 수 없습니다.");
        }
    }

    @Override
    public OauthUserInfo fetchUserInfo(String providerAccessToken) {
        try {
            KakaoUserInfoResponse response = restClient.get()
                    .uri(properties.userInfoUri())
                    .header("Authorization", "Bearer " + providerAccessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

            if (response == null || response.id() == null) {
                throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "Kakao 사용자 정보 응답이 비어있습니다.");
            }
            KakaoUserInfoResponse.KakaoAccount account = response.kakaoAccount();
            String email = account != null ? account.email() : null;
            Boolean emailVerified = account != null ? account.emailVerified() : null;
            String nickname = account != null && account.profile() != null ? account.profile().nickname() : null;
            String profileImageUrl = account != null && account.profile() != null
                    ? account.profile().profileImageUrl() : null;

            return new OauthUserInfo(String.valueOf(response.id()), email, emailVerified, nickname, profileImageUrl);
        } catch (RestClientException e) {
            log.error("Kakao 사용자 정보 조회 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "Kakao 사용자 정보를 가져올 수 없습니다.");
        }
    }

    @Override
    public void unlink(String providerUserId, String providerAccessToken) {
        // Kakao는 Admin Key + target_id(회원번호)만 있으면 사용자 토큰 없이도 unlink가 가능하다.
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("target_id_type", "user_id");
            form.add("target_id", providerUserId);

            restClient.post()
                    .uri(properties.unlinkUri())
                    .header("Authorization", "KakaoAK " + properties.adminKey())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("[{}] Kakao unlink 호출 실패. providerUserId={}", ErrorCode.OAUTH_UNLINK_FAILED.getCode(),
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
            log.warn("Kakao 인가 코드 검증 실패: {}", e.getResponseBodyAsString());
            return new BusinessException(ErrorCode.OAUTH_INVALID_CODE);
        }
        log.error("Kakao 토큰 교환 서버 오류: {}", e.getResponseBodyAsString(), e);
        return new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
    }
}
