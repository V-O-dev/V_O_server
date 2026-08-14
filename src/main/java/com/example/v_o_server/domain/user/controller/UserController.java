package com.example.v_o_server.domain.user.controller;

import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.domain.user.dto.request.UpdateNicknameRequest;
import com.example.v_o_server.domain.user.dto.request.UpdateNotificationSettingsRequest;
import com.example.v_o_server.domain.user.dto.response.NotificationSettingsResponse;
import com.example.v_o_server.domain.user.dto.response.ProfileCreateResponse;
import com.example.v_o_server.domain.user.dto.response.ProfileImageResponse;
import com.example.v_o_server.domain.user.dto.response.UserMeResponse;
import com.example.v_o_server.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Tag(name = "User", description = "사용자/프로필 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "닉네임, 프로필 이미지, 로그인 provider, 알림 설정을 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<UserMeResponse> getMe(@AuthenticationPrincipal Long userId) {
        UserMeResponse response = userService.getMe(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "온보딩 프로필 생성",
            description = "온보딩에서 입력한 이름과 프로필 사진으로 프로필을 생성합니다. 사진은 선택 사항입니다.")
    @PostMapping(value = "/me/profile", consumes = "multipart/form-data")
    public ApiResponse<ProfileCreateResponse> createProfile(@AuthenticationPrincipal Long userId,
            @RequestParam("nickname") String nickname,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        ProfileCreateResponse response = userService.createProfile(userId, nickname, image);
        return ApiResponse.success(response);
    }

    @Operation(summary = "알림 설정 변경", description = "오늘의 질문 알림, 댓글·좋아요 알림 수신 여부를 변경합니다.")
    @PatchMapping("/me/notification-settings")
    public ApiResponse<NotificationSettingsResponse> updateNotificationSettings(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody UpdateNotificationSettingsRequest request) {
        NotificationSettingsResponse response = userService.updateNotificationSettings(userId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "닉네임 수정", description = "사용자의 닉네임을 설정하거나 수정합니다.")
    @PatchMapping("/me/profile")
    public ApiResponse<String> updateNickname(@AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateNicknameRequest request) {
        String nickname = userService.updateNickname(userId, request.nickname());
        return ApiResponse.success(nickname);
    }

    @Operation(summary = "프로필 이미지 등록/수정", description = "프로필 이미지를 업로드하여 등록하거나 기존 이미지를 교체합니다.")
    @PatchMapping(value = "/me/profile/image", consumes = "multipart/form-data")
    public ApiResponse<ProfileImageResponse> updateProfileImage(@AuthenticationPrincipal Long userId,
            @RequestPart("image") MultipartFile image) {
        ProfileImageResponse response = userService.updateProfileImage(userId, image);
        return ApiResponse.success(response);
    }

    @Operation(summary = "프로필 이미지 삭제", description = "등록된 프로필 이미지를 삭제합니다.")
    @DeleteMapping("/me/profile/image")
    public ApiResponse<ProfileImageResponse> deleteProfileImage(@AuthenticationPrincipal Long userId) {
        ProfileImageResponse response = userService.deleteProfileImage(userId);
        return ApiResponse.success(response);
    }

    /**
     * 이미지 파트가 아예 없거나 파일이 아닌 값으로 온 경우를 이 도메인의 검증 코드로 통일한다.
     *
     * <p>사용자 입장에서 "파일을 안 보냄", "빈 값을 보냄", "0바이트 파일을 보냄"은 모두 같은 상황이다.
     * 앞의 둘은 파트 자체가 성립하지 않아 전역 핸들러의 {@code C008}로, 마지막은 서비스 검증의
     * {@code U008}로 갈렸는데, 여기서 앞의 둘을 {@code U008}로 맞춰 셋을 같은 응답으로 만든다.</p>
     *
     * <p>이 방식을 쓰는 이유는 {@code @RequestPart}를 {@code required = true}로 유지하기 위해서다.
     * springdoc은 multipart 스키마의 {@code required}를 {@code @RequestPart.required()}에서만 읽으므로,
     * 이를 {@code false}로 바꾸면 OpenAPI에서 "이미지는 필수" 표기가 사라진다.
     * 즉 <b>문서 계약과 응답 코드 통일을 모두 지키기 위한</b> 컨트롤러 한정 처리다.</p>
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingImagePart(MissingServletRequestPartException e) {
        return ResponseEntity.status(ErrorCode.IMAGE_REQUIRED.getStatus())
                .body(ApiResponse.error(ErrorCode.IMAGE_REQUIRED));
    }
}