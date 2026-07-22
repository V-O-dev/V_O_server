package com.example.v_o_server.domain.user.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.domain.user.dto.request.UpdateNicknameRequest;
import com.example.v_o_server.domain.user.dto.request.UpdateNotificationSettingsRequest;
import com.example.v_o_server.domain.user.dto.response.NotificationSettingsResponse;
import com.example.v_o_server.domain.user.dto.response.ProfileImageResponse;
import com.example.v_o_server.domain.user.dto.response.UserMeResponse;
import com.example.v_o_server.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User", description = "사용자/프로필 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "닉네임, 프로필 이미지, 로그인 provider, 알림 설정을 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<UserMeResponse> getMe() {
        Long tempUserId = 1L;
        UserMeResponse response = userService.getMe(tempUserId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "알림 설정 변경", description = "오늘의 질문 알림, 댓글·좋아요 알림 수신 여부를 변경합니다.")
    @PatchMapping("/me/notification-settings")
    public ApiResponse<NotificationSettingsResponse> updateNotificationSettings(
            @Valid @RequestBody UpdateNotificationSettingsRequest request) {
        Long tempUserId = 1L;
        NotificationSettingsResponse response = userService.updateNotificationSettings(tempUserId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "닉네임 수정", description = "사용자의 닉네임을 설정하거나 수정합니다.")
    @PatchMapping("/me/profile")
    public ApiResponse<String> updateNickname(@Valid @RequestBody UpdateNicknameRequest request) {
        Long tempUserId = 1L;
        String nickname = userService.updateNickname(tempUserId, request.nickname());
        return ApiResponse.success(nickname);
    }

    @Operation(summary = "프로필 이미지 등록/수정", description = "프로필 이미지를 업로드하여 등록하거나 기존 이미지를 교체합니다.")
    @PatchMapping(value = "/me/profile/image", consumes = "multipart/form-data")
    public ApiResponse<ProfileImageResponse> updateProfileImage(@RequestPart("image") MultipartFile image) {
        Long tempUserId = 1L;
        ProfileImageResponse response = userService.updateProfileImage(tempUserId, image);
        return ApiResponse.success(response);
    }

    @Operation(summary = "프로필 이미지 삭제", description = "등록된 프로필 이미지를 삭제합니다.")
    @DeleteMapping("/me/profile/image")
    public ApiResponse<ProfileImageResponse> deleteProfileImage() {
        Long tempUserId = 1L;
        ProfileImageResponse response = userService.deleteProfileImage(tempUserId);
        return ApiResponse.success(response);
    }
}