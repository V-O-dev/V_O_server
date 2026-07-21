package com.example.v_o_server.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
            @JsonProperty("email") String email,
            @JsonProperty("is_email_verified") Boolean emailVerified,
            @JsonProperty("profile") Profile profile
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(
            @JsonProperty("nickname") String nickname,
            @JsonProperty("profile_image_url") String profileImageUrl
    ) {
    }
}
