package com.example.v_o_server.domain.auth.client;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.auth.client.dto.NaverTokenResponse;
import com.example.v_o_server.domain.auth.client.dto.NaverUserInfoResponse;
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

/**
 * Naver OAuth 연동.
 * <p>공식 문서: <a href="https://developers.naver.com/docs/login/api/api.md">네이버 로그인 API</a>
 * TODO: 실제 client-id/secret 발급 후 토큰 교환 및 사용자 정보 응답 스펙이 최신인지 확인 필요</p>
 */
@Slf4j
@Component
public class NaverOauthApiClient implements OauthApiClient {

    private final NaverOauthProperties properties;
    private final RestClient restClient = RestClient.create();

    public NaverOauthApiClient(NaverOauthProperties properties) {
        this.properties = properties;
    }

    @Override
    public OauthProvider getProvider() {
        return OauthProvider.NAVER;
    }

    /**
     * {@code redirectUri}는 사용하지 않는다. 네이버 토큰 발급 API는 다른 provider와 달리
     * redirect_uri를 파라미터로 받지 않고 state로 요청 일치를 검증한다.
     * (authorize 단계에서 쓴 콜백 주소는 네이버 콘솔 등록값으로만 검증된다)
     */
    @Override
    public OauthTokenResult exchangeToken(String authorizationCode, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("code", authorizationCode);
        // TODO: 프론트에서 state를 함께 전달받는 흐름이 확정되면 state 파라미터 추가 (CSRF 검증 강화)

        try {
            NaverTokenResponse response = restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(NaverTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                // 네이버는 잘못된 인가 코드에도 HTTP 200 + error 필드로 응답할 수 있다
                String error = response == null ? null : response.error();
                log.warn("Naver 토큰 교환 실패: error={}, description={}",
                        error, response == null ? null : response.errorDescription());
                if (error != null) {
                    throw new BusinessException(ErrorCode.OAUTH_INVALID_CODE);
                }
                throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "Naver 토큰 응답이 비어있습니다.");
            }
            return new OauthTokenResult(response.accessToken(), response.refreshToken(), response.expiresIn());
        } catch (RestClientResponseException e) {
            throw mapTokenExchangeError(e);
        } catch (RestClientException e) {
            log.error("Naver 토큰 교환 통신 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "Naver 서버와 통신할 수 없습니다.");
        }
    }

    @Override
    public OauthUserInfo fetchUserInfo(String providerAccessToken) {
        try {
            NaverUserInfoResponse response = restClient.get()
                    .uri(properties.userInfoUri())
                    .header("Authorization", "Bearer " + providerAccessToken)
                    .retrieve()
                    .body(NaverUserInfoResponse.class);

            if (response == null || response.response() == null || response.response().id() == null) {
                throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "Naver 사용자 정보 응답이 비어있습니다.");
            }
            NaverUserInfoResponse.NaverAccount account = response.response();
            // 네이버는 이메일 인증 여부를 내려주지 않는다
            return new OauthUserInfo(account.id(), account.email(), null,
                    account.nickname(), account.profileImage());
        } catch (RestClientException e) {
            log.error("Naver 사용자 정보 조회 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR, "Naver 사용자 정보를 가져올 수 없습니다.");
        }
    }

    @Override
    public void unlink(String providerUserId, String providerAccessToken) {
        // 구조적 한계: 네이버 연결 해제(grant_type=delete)는 사용자 access token이 필요한데,
        // provider 토큰을 저장하지 않으므로 탈퇴 시점엔 호출할 토큰이 없다 (Google과 동일한 케이스).
        if (providerAccessToken == null) {
            log.error("[{}] Naver unlink 실패: 저장된 provider token이 없어 연결 해제를 시도할 수 없습니다. providerUserId={}",
                    ErrorCode.OAUTH_UNLINK_FAILED.getCode(), providerUserId);
            return;
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "delete");
            form.add("client_id", properties.clientId());
            form.add("client_secret", properties.clientSecret());
            form.add("access_token", providerAccessToken);
            form.add("service_provider", "NAVER");

            restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("[{}] Naver unlink 호출 실패. providerUserId={}", ErrorCode.OAUTH_UNLINK_FAILED.getCode(),
                    providerUserId, e);
        }
    }

    private BusinessException mapTokenExchangeError(RestClientResponseException e) {
        HttpStatusCode status = e.getStatusCode();
        if (status.is4xxClientError()) {
            log.warn("Naver 인가 코드 검증 실패: {}", e.getResponseBodyAsString());
            return new BusinessException(ErrorCode.OAUTH_INVALID_CODE);
        }
        log.error("Naver 토큰 교환 서버 오류: {}", e.getResponseBodyAsString(), e);
        return new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
    }
}
