package com.example.v_o_server.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.jwt.JwtProperties;
import com.example.v_o_server.common.jwt.JwtProvider;
import com.example.v_o_server.common.jwt.TokenBlacklistService;
import com.example.v_o_server.domain.auth.client.OauthApiClient;
import com.example.v_o_server.domain.auth.entity.AuthOauthAccount;
import com.example.v_o_server.domain.auth.entity.OauthAccountStatus;
import com.example.v_o_server.domain.auth.entity.OauthProvider;
import com.example.v_o_server.domain.auth.repository.AuthOauthAccountRepository;
import com.example.v_o_server.domain.auth.repository.AuthRefreshTokenRepository;
import com.example.v_o_server.domain.group.entity.GroupStatus;
import com.example.v_o_server.domain.group.repository.PrivateGroupRepository;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.entity.UserStatus;
import com.example.v_o_server.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("AuthService 회원 탈퇴")
class AuthServiceWithdrawalTest {

    private static final Long USER_ID = 1L;

    private UserRepository userRepository;
    private AuthOauthAccountRepository authOauthAccountRepository;
    private AuthRefreshTokenRepository authRefreshTokenRepository;
    private PrivateGroupRepository privateGroupRepository;
    private OauthApiClient kakaoClient;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        authOauthAccountRepository = org.mockito.Mockito.mock(AuthOauthAccountRepository.class);
        authRefreshTokenRepository = org.mockito.Mockito.mock(AuthRefreshTokenRepository.class);
        privateGroupRepository = org.mockito.Mockito.mock(PrivateGroupRepository.class);
        kakaoClient = org.mockito.Mockito.mock(OauthApiClient.class);
        given(kakaoClient.getProvider()).willReturn(OauthProvider.KAKAO);

        authService = new AuthService(
                List.of(kakaoClient),
                userRepository,
                authOauthAccountRepository,
                authRefreshTokenRepository,
                privateGroupRepository,
                org.mockito.Mockito.mock(JwtProvider.class),
                new JwtProperties("test-secret", 3600, 1209600),
                org.mockito.Mockito.mock(TokenBlacklistService.class)
        );
    }

    @Test
    @DisplayName("탈퇴 시 사용자와 연결 데이터는 삭제하지 않고 soft delete와 OAuth unlink만 수행한다")
    void withdrawKeepsUserDataAndUnlinksOauthAccount() {
        User user = user();
        AuthOauthAccount account = oauthAccount(user);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(privateGroupRepository.existsByOwnerAndStatus(user, GroupStatus.ACTIVE)).willReturn(false);
        given(authOauthAccountRepository.findAllByUser(user)).willReturn(List.of(account));

        authService.withdraw(USER_ID);

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.getWithdrawnAt()).isNotNull();
        assertThat(account.getStatus()).isEqualTo(OauthAccountStatus.UNLINKED);
        assertThat(account.getUnlinkedAt()).isEqualTo(user.getWithdrawnAt());
        verify(kakaoClient).unlink("kakao-user-1", null);

        // 사용자 행을 hard delete하지 않으므로 FK로 연결된 영상·댓글·아카이브도 유지된다.
        verify(userRepository, never()).delete(any(User.class));
        verify(userRepository, never()).deleteById(any(Long.class));
        verify(userRepository, never()).deleteAllById(any());
        verify(authOauthAccountRepository, never()).delete(any(AuthOauthAccount.class));
    }

    @Test
    @DisplayName("ACTIVE 그룹의 방장인 사용자는 데이터 변경 없이 탈퇴가 거부된다")
    void rejectsWithdrawalWhenUserOwnsActiveGroup() {
        User user = user();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(privateGroupRepository.existsByOwnerAndStatus(user, GroupStatus.ACTIVE)).willReturn(true);

        BusinessException exception = catchThrowableOfType(
                () -> authService.withdraw(USER_ID),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.WITHDRAW_OWNER_EXISTS);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getWithdrawnAt()).isNull();
        verifyNoInteractions(authOauthAccountRepository);
        verify(kakaoClient, never()).unlink(any(), any());
    }

    private User user() {
        User user = User.builder()
                .status(UserStatus.ACTIVE)
                .dailyQuestionNotificationEnabled(true)
                .interactionNotificationEnabled(true)
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private AuthOauthAccount oauthAccount(User user) {
        return AuthOauthAccount.builder()
                .provider(OauthProvider.KAKAO)
                .providerUserId("kakao-user-1")
                .status(OauthAccountStatus.ACTIVE)
                .user(user)
                .build();
    }
}
