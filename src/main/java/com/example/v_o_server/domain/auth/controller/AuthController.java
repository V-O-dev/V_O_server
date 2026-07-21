package com.example.v_o_server.domain.auth.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.domain.auth.dto.request.LoginRequest;
import com.example.v_o_server.domain.auth.dto.request.LogoutRequest;
import com.example.v_o_server.domain.auth.dto.request.RefreshRequest;
import com.example.v_o_server.domain.auth.dto.response.LoginResponse;
import com.example.v_o_server.domain.auth.dto.response.RefreshResponse;
import com.example.v_o_server.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "OAuth 소셜 로그인/인증")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    @Operation(summary = "소셜 로그인", description = "provider 인가 코드로 로그인하고 서비스 JWT를 발급한다.")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "토큰 재발급", description = "refreshToken을 검증해 accessToken/refreshToken을 재발급한다 (rotation).")
    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @Operation(summary = "로그아웃", description = "accessToken을 블랙리스트에 등록하고 refreshToken을 폐기한다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody LogoutRequest request) {
        String accessToken = authorizationHeader.startsWith(BEARER_PREFIX)
                ? authorizationHeader.substring(BEARER_PREFIX.length())
                : authorizationHeader;
        authService.logout(accessToken, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "회원 탈퇴", description = "본인 계정을 soft delete 처리하고 OAuth 연결을 해제한다.")
    @DeleteMapping("/withdraw")
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal Long userId) {
        authService.withdraw(userId);
        return ApiResponse.ok();
    }
}
