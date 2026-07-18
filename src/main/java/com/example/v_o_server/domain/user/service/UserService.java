package com.example.v_o_server.domain.user.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.auth.entity.AuthOauthAccount;
import com.example.v_o_server.domain.auth.repository.AuthOauthAccountRepository;
import com.example.v_o_server.domain.user.dto.response.UserMeResponse;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.entity.UserProfile;
import com.example.v_o_server.domain.user.repository.UserProfileRepository;
import com.example.v_o_server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthOauthAccountRepository authOauthAccountRepository;

    public UserMeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

        String provider = authOauthAccountRepository.findByUserId(userId)
                .map(AuthOauthAccount::getProvider)
                .map(Enum::name)
                .orElse(null);

        return new UserMeResponse(
                user.getId(),
                profile.getNickname(),
                profile.getProfileImageUrl(),
                provider,
                user.getDailyQuestionNotificationEnabled(),
                user.getInteractionNotificationEnabled()
        );
    }
}