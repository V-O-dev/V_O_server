package com.example.v_o_server.domain.auth.controller;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.config.FrontendProperties;
import com.example.v_o_server.domain.auth.dto.request.LoginRequest;
import com.example.v_o_server.domain.auth.dto.request.LogoutRequest;
import com.example.v_o_server.domain.auth.dto.request.RefreshRequest;
import com.example.v_o_server.domain.auth.dto.response.LoginResponse;
import com.example.v_o_server.domain.auth.dto.response.RefreshResponse;
import com.example.v_o_server.domain.auth.entity.OauthProvider;
import com.example.v_o_server.domain.auth.service.AuthService;
import com.example.v_o_server.domain.auth.service.FrontendRedirectValidator;
import com.example.v_o_server.domain.auth.service.OauthLoginStateStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Tag(name = "Auth", description = "OAuth 소셜 로그인/인증")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";
    /** 테스트용 콜백 경로. 로그인 진입점과 콜백이 같은 주소를 쓰도록 상수로 공유한다. */
    private static final String DEV_CALLBACK_PATH = "/api/v1/auth/dev/callback/";

    private final AuthService authService;
    private final OauthLoginStateStore oauthLoginStateStore;
    private final FrontendRedirectValidator frontendRedirectValidator;
    private final FrontendProperties frontendProperties;

    @Operation(summary = "소셜 로그인", description = "provider 인가 코드로 로그인하고 서비스 JWT를 발급한다.")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 프론트가 사용하는 소셜 로그인 진입점.
     *
     * <p>사용자를 provider 로그인 화면으로 보내고, 로그인이 끝나면 아래 콜백을 거쳐
     * 프론트 주소로 토큰과 함께 리다이렉트한다. 프론트는 이 링크로 이동시키기만 하면 된다.</p>
     *
     * @param redirectUri 로그인 후 돌아갈 프론트 주소. 생략하면 서버 기본값(app.frontend.base-url)을 쓴다.
     *                    로컬/Vercel을 동시에 지원하기 위해 요청값을 우선한다.
     */
    @Operation(summary = "소셜 로그인 시작(리다이렉트)",
            description = "provider 로그인 화면으로 이동시킨다. 완료 후 프론트 주소로 토큰을 쿼리스트링에 담아 리다이렉트한다.")
    @GetMapping("/oauth/{provider}/login")
    public ResponseEntity<Void> oauthLogin(@PathVariable String provider,
            @RequestParam(value = "redirectUri", required = false) String redirectUri) {
        OauthProvider oauthProvider = OauthProvider.valueOf(provider.toUpperCase(Locale.ROOT));

        String frontendRedirectUri = resolveFrontendRedirectUri(provider, redirectUri);
        frontendRedirectValidator.validate(frontendRedirectUri);

        // 콜백에서 "어느 프론트로 돌려보낼지" 복원하기 위해 state에 묶어둔다.
        String state = oauthLoginStateStore.issue(frontendRedirectUri);

        String authorizeUrl = authService.buildAuthorizeUrl(
                oauthProvider, buildOauthCallbackUri(provider), state);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authorizeUrl))
                .build();
    }

    /**
     * provider가 인가 코드를 돌려주는 지점. 로그인 처리 후 프론트로 302 리다이렉트한다.
     *
     * <p>성공: {@code {프론트주소}?accessToken=...&refreshToken=...&isNewUser=...}<br>
     * 실패: {@code {프론트주소}?error=에러코드} — 프론트가 실패도 화면에서 처리할 수 있게 한다.</p>
     */
    @Operation(summary = "소셜 로그인 콜백(리다이렉트)",
            description = "provider 리다이렉트 수신 전용. 브라우저가 호출하며 프론트로 다시 리다이렉트된다.")
    @GetMapping("/oauth/{provider}/callback")
    public ResponseEntity<Void> oauthCallback(@PathVariable String provider,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String providerError) {

        // state는 우리가 발급한 1회용 값이어야 한다. 없거나 모르는 값이면 로그인 CSRF 시도일 수 있으므로
        // 인가 코드가 있어도 로그인을 진행하지 않는다. 돌아갈 곳을 모르니 서버 기본 주소로 보낸다.
        var storedRedirectUri = oauthLoginStateStore.consume(state);
        if (storedRedirectUri.isEmpty()) {
            log.warn("OAuth 콜백 state 검증 실패: provider={}, state={}", provider, state);
            return redirectTo(errorRedirectUrl(
                    resolveFrontendRedirectUri(provider, null), ErrorCode.OAUTH_INVALID_STATE.getCode()));
        }
        String frontendRedirectUri = storedRedirectUri.get();

        // 사용자가 provider 화면에서 취소한 경우 등
        if (providerError != null || code == null || code.isBlank()) {
            log.warn("OAuth 콜백 실패: provider={}, error={}", provider, providerError);
            return redirectTo(errorRedirectUrl(frontendRedirectUri, ErrorCode.OAUTH_INVALID_CODE.getCode()));
        }

        try {
            OauthProvider oauthProvider = OauthProvider.valueOf(provider.toUpperCase(Locale.ROOT));
            LoginResponse login = authService.login(
                    new LoginRequest(oauthProvider, code, buildOauthCallbackUri(provider)));

            String target = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                    .queryParam("accessToken", login.accessToken())
                    .queryParam("refreshToken", login.refreshToken())
                    .queryParam("isNewUser", login.isNewUser())
                    .encode()
                    .toUriString();
            return redirectTo(target);
        } catch (BusinessException e) {
            log.warn("OAuth 로그인 처리 실패: provider={}, code={}", provider, e.getErrorCode().getCode());
            return redirectTo(errorRedirectUrl(frontendRedirectUri, e.getErrorCode().getCode()));
        }
    }

    /** 이 서버의 OAuth 콜백 주소. authorize와 토큰 교환에서 동일한 값이 쓰이도록 한 곳에서 만든다. */
    private String buildOauthCallbackUri(String provider) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/auth/oauth/")
                .path(provider.toLowerCase(Locale.ROOT))
                .path("/callback")
                .toUriString();
    }

    private String resolveFrontendRedirectUri(String provider, String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        return frontendProperties.baseUrl()
                + frontendProperties.oauthCallbackPath()
                + provider.toLowerCase(Locale.ROOT);
    }

    private String errorRedirectUrl(String frontendRedirectUri, String errorCode) {
        return UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("error", errorCode)
                .encode()
                .toUriString();
    }

    private ResponseEntity<Void> redirectTo(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    /**
     * 백엔드 단독 테스트용 로그인 진입점.
     * 이 주소로 접속하면 provider 로그인 페이지로 리다이렉트되고, 로그인 후 아래 콜백이 토큰을 반환한다.
     * 긴 authorize URL을 직접 조립할 필요가 없다.
     */
    @Operation(summary = "[테스트용] 소셜 로그인 시작",
            description = "브라우저로 접속하면 provider 로그인 화면으로 이동한다. 로그인하면 토큰이 JSON으로 표시된다.")
    @GetMapping("/dev/login/{provider}")
    public ResponseEntity<Void> devLogin(@PathVariable String provider) {
        OauthProvider oauthProvider = OauthProvider.valueOf(provider.toUpperCase(Locale.ROOT));

        // 콜백 주소는 이 서버의 dev 콜백. authorize와 토큰 교환에서 같은 값이 쓰이도록 여기서 한 번만 만든다.
        String callbackUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(DEV_CALLBACK_PATH)
                .path(provider.toLowerCase(Locale.ROOT))
                .toUriString();

        String authorizeUrl = authService.buildAuthorizeUrl(
                oauthProvider, callbackUri, UUID.randomUUID().toString());

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authorizeUrl))
                .build();
    }

    /**
     * 백엔드 단독 테스트용 콜백. 프론트 없이 브라우저에서 provider 로그인만 하면 토큰을 받아볼 수 있다.
     *
     * <p>사용법: provider 콘솔에 이 주소를 Redirect URI로 등록한 뒤,
     * authorize URL의 redirect_uri에 같은 값을 넣고 브라우저로 접속하면
     * 로그인 완료 후 이 엔드포인트가 토큰을 JSON으로 반환한다.</p>
     *
     * <p>인가 코드 없이는 아무것도 할 수 없어(=실제 provider 인증 필수) POST /login과 권한이 동일하다.
     * 즉 인증 우회 통로가 아니며, 단지 브라우저 리다이렉트를 받아주는 편의 기능이다.</p>
     */
    @Operation(summary = "[테스트용] OAuth 콜백 수신", description = "프론트 없이 토큰을 발급받기 위한 개발 편의용 엔드포인트.")
    @GetMapping("/dev/callback/{provider}")
    public ApiResponse<LoginResponse> devCallback(@PathVariable String provider,
            @RequestParam("code") String code) {
        // 토큰 교환 시 provider가 authorize 때 쓴 값과 동일한지 검증하므로,
        // 지금 요청이 들어온 주소(쿼리 제외)를 그대로 redirect_uri로 사용한다.
        String redirectUri = ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString();

        OauthProvider oauthProvider = OauthProvider.valueOf(provider.toUpperCase(Locale.ROOT));
        return ApiResponse.success(authService.login(new LoginRequest(oauthProvider, code, redirectUri)));
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
