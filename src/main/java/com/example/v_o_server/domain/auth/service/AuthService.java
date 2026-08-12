package com.example.v_o_server.domain.auth.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.jwt.JwtProperties;
import com.example.v_o_server.common.jwt.JwtProvider;
import com.example.v_o_server.common.jwt.TokenBlacklistService;
import com.example.v_o_server.domain.auth.client.OauthApiClient;
import com.example.v_o_server.domain.auth.client.OauthTokenResult;
import com.example.v_o_server.domain.auth.client.OauthUserInfo;
import com.example.v_o_server.domain.auth.dto.request.LoginRequest;
import com.example.v_o_server.domain.auth.dto.request.LogoutRequest;
import com.example.v_o_server.domain.auth.dto.request.RefreshRequest;
import com.example.v_o_server.domain.auth.dto.response.LoginResponse;
import com.example.v_o_server.domain.auth.dto.response.RefreshResponse;
import com.example.v_o_server.domain.auth.entity.AuthOauthAccount;
import com.example.v_o_server.domain.auth.entity.AuthRefreshToken;
import com.example.v_o_server.domain.auth.entity.OauthAccountStatus;
import com.example.v_o_server.domain.auth.entity.OauthProvider;
import com.example.v_o_server.domain.auth.repository.AuthOauthAccountRepository;
import com.example.v_o_server.domain.auth.repository.AuthRefreshTokenRepository;
import com.example.v_o_server.domain.group.entity.GroupStatus;
import com.example.v_o_server.domain.group.repository.PrivateGroupRepository;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.entity.UserStatus;
import com.example.v_o_server.domain.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Map<OauthProvider, OauthApiClient> oauthApiClients;
    private final UserRepository userRepository;
    private final AuthOauthAccountRepository authOauthAccountRepository;
    private final AuthRefreshTokenRepository authRefreshTokenRepository;
    private final PrivateGroupRepository privateGroupRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(List<OauthApiClient> oauthApiClients, UserRepository userRepository,
            AuthOauthAccountRepository authOauthAccountRepository,
            AuthRefreshTokenRepository authRefreshTokenRepository,
            PrivateGroupRepository privateGroupRepository, JwtProvider jwtProvider,
            JwtProperties jwtProperties, TokenBlacklistService tokenBlacklistService) {
        this.oauthApiClients = oauthApiClients.stream()
                .collect(Collectors.toMap(OauthApiClient::getProvider, Function.identity()));
        this.userRepository = userRepository;
        this.authOauthAccountRepository = authOauthAccountRepository;
        this.authRefreshTokenRepository = authRefreshTokenRepository;
        this.privateGroupRepository = privateGroupRepository;
        this.jwtProvider = jwtProvider;
        this.jwtProperties = jwtProperties;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * provider 로그인 페이지 URL을 만든다. (테스트용 로그인 진입점에서 사용)
     */
    @Transactional(readOnly = true)
    public String buildAuthorizeUrl(OauthProvider provider, String redirectUri, String state) {
        OauthApiClient client = oauthApiClients.get(provider);
        if (client == null) {
            throw new BusinessException(ErrorCode.OAUTH_UNSUPPORTED_PROVIDER);
        }
        return client.buildAuthorizeUrl(redirectUri, state);
    }

    public LoginResponse login(LoginRequest request) {
        OauthApiClient client = oauthApiClients.get(request.provider());
        if (client == null) {
            throw new BusinessException(ErrorCode.OAUTH_UNSUPPORTED_PROVIDER);
        }

        // 프론트 실행 환경(localhost / Vercel)마다 콜백 주소가 달라 요청값을 그대로 전달한다.
        OauthTokenResult tokenResult = client.exchangeToken(request.authorizationCode(), request.redirectUri());
        OauthUserInfo userInfo = client.fetchUserInfo(tokenResult.accessToken());

        LocalDateTime now = LocalDateTime.now();
        var existing = authOauthAccountRepository.findByProviderAndProviderUserId(
                request.provider(), userInfo.providerUserId());

        User user;
        boolean isNewUser;
        if (existing.isPresent()) {
            AuthOauthAccount account = existing.get();
            if (account.getStatus() == OauthAccountStatus.UNLINKED) {
                account.relink(now);
            } else {
                account.recordLogin(now);
            }
            user = account.getUser();
            // 탈퇴했던 계정으로 다시 로그인한 경우. 되살리지 않으면 계정이 WITHDRAWN으로 남아
            // 로그인은 되는데 모든 API가 401로 막히는 상태가 된다.
            if (user.isWithdrawn()) {
                user.reactivate();
            }
            user.updateLastLoginAt(now);
            isNewUser = false;
        } else {
            user = userRepository.save(User.builder()
                    .status(UserStatus.ACTIVE)
                    .dailyQuestionNotificationEnabled(true)
                    .interactionNotificationEnabled(true)
                    .build());

            AuthOauthAccount account = AuthOauthAccount.builder()
                    .provider(request.provider())
                    .providerUserId(userInfo.providerUserId())
                    .providerEmail(userInfo.email())
                    .providerEmailVerified(userInfo.emailVerified())
                    .providerDisplayName(userInfo.displayName())
                    .providerProfileImageUrl(userInfo.profileImageUrl())
                    .status(OauthAccountStatus.ACTIVE)
                    .user(user)
                    .build();
            account.recordLogin(now);
            authOauthAccountRepository.save(account);

            isNewUser = true;
        }

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = issueRefreshToken(user, now);

        return new LoginResponse(accessToken, refreshToken, isNewUser);
    }

    public RefreshResponse refresh(RefreshRequest request) {
        String hash = hash(request.refreshToken());
        AuthRefreshToken savedToken = authRefreshTokenRepository.findByRefreshTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (savedToken.getRevokedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (savedToken.getExpiresAt().isBefore(now)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        savedToken.revoke(now, "ROTATED");

        User user = savedToken.getUser();
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = issueRefreshToken(user, now);

        return new RefreshResponse(accessToken, refreshToken);
    }

    public void logout(String rawAccessToken, LogoutRequest request) {
        blacklistAccessToken(rawAccessToken);

        authRefreshTokenRepository.findByRefreshTokenHash(hash(request.refreshToken()))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.revoke(LocalDateTime.now(), "LOGOUT"));
    }

    /**
     * 회원 탈퇴. 계정을 soft delete하고 <b>모든 기기의 인증 수단을 무효화</b>한다.
     *
     * @param rawAccessToken 현재 요청에 쓰인 accessToken. 즉시 차단하기 위해 블랙리스트에 넣는다.
     */
    public void withdraw(Long userId, String rawAccessToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (privateGroupRepository.existsByOwnerAndStatus(user, GroupStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.WITHDRAW_OWNER_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        user.withdraw(now);

        // 현재 기기의 accessToken은 남은 유효시간 동안 통과되므로 즉시 폐기한다(logout과 동일).
        blacklistAccessToken(rawAccessToken);

        // refreshToken을 남겨두면 탈퇴 후에도 /auth/refresh로 새 accessToken을 계속 받을 수 있다.
        // 특정 기기가 아니라 이 사용자의 모든 활성 토큰을 끊는다.
        authRefreshTokenRepository.findAllByUserAndRevokedAtIsNull(user)
                .forEach(token -> token.revoke(now, "WITHDRAW"));

        List<AuthOauthAccount> accounts = authOauthAccountRepository.findAllByUser(user);
        for (AuthOauthAccount account : accounts) {
            account.unlink(now);
            OauthApiClient client = oauthApiClients.get(account.getProvider());
            if (client == null) {
                continue;
            }
            try {
                // 로그인 시점에만 존재하던 provider access token은 저장하지 않으므로 여기서는 항상 null이다.
                // Kakao는 Admin Key 기반이라 문제없이 동작하지만, Google은 토큰이 없어 구조적으로 실패한다 (계획 문서 참고).
                client.unlink(account.getProviderUserId(), null);
            } catch (Exception e) {
                log.error("[{}] provider unlink 처리 중 예외. provider={}, providerUserId={}",
                        ErrorCode.OAUTH_UNLINK_FAILED.getCode(), account.getProvider(),
                        account.getProviderUserId(), e);
            }
        }
    }

    /**
     * accessToken을 남은 유효시간만큼 블랙리스트에 등록한다.
     * 이미 만료·무효한 토큰이면 조용히 넘어간다(로그아웃/탈퇴 모두 멱등해야 하므로).
     */
    private void blacklistAccessToken(String rawAccessToken) {
        if (rawAccessToken == null || rawAccessToken.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtProvider.parse(rawAccessToken);
            tokenBlacklistService.blacklist(jwtProvider.getJti(claims), jwtProvider.getRemainingSeconds(claims));
        } catch (JwtException e) {
            log.debug("이미 만료/무효한 accessToken (멱등 처리): {}", e.getMessage());
        }
    }

    private String issueRefreshToken(User user, LocalDateTime issuedAt) {
        String rawToken = generateOpaqueToken();
        AuthRefreshToken refreshToken = AuthRefreshToken.builder()
                .user(user)
                .refreshTokenHash(hash(rawToken))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(jwtProperties.refreshTokenValiditySeconds()))
                .build();
        authRefreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
