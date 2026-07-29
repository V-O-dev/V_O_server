package com.example.v_o_server.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 네이버는 인가 코드가 잘못돼도 HTTP 200에 error 필드로 응답하는 경우가 있어 error 필드를 함께 매핑한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("error") String error,
        @JsonProperty("error_description") String errorDescription
) {
}
