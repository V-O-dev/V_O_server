package com.example.v_o_server.domain.user.dto.response;

import com.example.v_o_server.domain.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "온보딩 프로필 생성 결과")
public record ProfileCreateResponse(

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "닉네임", example = "홍길동")
        String nickname,

        @Schema(description = "프로필 이미지 URL. 사진을 건너뛴 경우 null")
        String profileImageUrl
) {

    public static ProfileCreateResponse from(UserProfile profile) {
        return new ProfileCreateResponse(
                profile.getId(),
                profile.getNickname(),
                profile.getProfileImageUrl()
        );
    }
}
