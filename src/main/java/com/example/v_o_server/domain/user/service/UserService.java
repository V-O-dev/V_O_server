package com.example.v_o_server.domain.user.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.storage.FileStorageService;
import com.example.v_o_server.common.storage.FileStorageService.StoredFile;
import com.example.v_o_server.domain.auth.entity.AuthOauthAccount;
import com.example.v_o_server.domain.auth.entity.OauthAccountStatus;
import com.example.v_o_server.domain.auth.repository.AuthOauthAccountRepository;
import com.example.v_o_server.domain.user.dto.request.UpdateNotificationSettingsRequest;
import com.example.v_o_server.domain.user.dto.response.NotificationSettingsResponse;
import com.example.v_o_server.domain.user.dto.response.ProfileImageResponse;
import com.example.v_o_server.domain.user.dto.response.UserMeResponse;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.entity.UserProfile;
import com.example.v_o_server.domain.user.repository.UserProfileRepository;
import com.example.v_o_server.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");
    private static final String PROFILE_IMAGE_DIR = "profile-images";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthOauthAccountRepository authOauthAccountRepository;
    private final FileStorageService fileStorageService;

    public UserMeResponse getMe(Long userId) {
        User user = getUser(userId);
        UserProfile profile = getProfile(userId);

        String provider = authOauthAccountRepository.findFirstByUserAndStatus(user, OauthAccountStatus.ACTIVE)
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

    @Transactional
    public String updateNickname(Long userId, String nickname) {
        UserProfile profile = getProfile(userId);
        profile.updateNickname(nickname);
        return profile.getNickname();
    }

    @Transactional
    public NotificationSettingsResponse updateNotificationSettings(Long userId, UpdateNotificationSettingsRequest request) {
        User user = getUser(userId);
        user.updateNotificationSettings(request.questionNotification(), request.interactionNotification());

        return new NotificationSettingsResponse(
                user.getDailyQuestionNotificationEnabled(),
                user.getInteractionNotificationEnabled()
        );
    }

    @Transactional
    public ProfileImageResponse updateProfileImage(Long userId, MultipartFile image) {
        validateImage(image);

        UserProfile profile = getProfile(userId);

        // 기존 이미지가 있으면 먼저 삭제 (정합성: 업로드 성공 후 기존 파일 정리)
        String previousObjectKey = profile.getProfileImageObjectKey();

        StoredFile stored = fileStorageService.upload(image, PROFILE_IMAGE_DIR);
        profile.updateProfileImage(stored.url(), stored.objectKey());

        if (previousObjectKey != null) {
            fileStorageService.delete(previousObjectKey);
        }

        return new ProfileImageResponse(stored.url());
    }

    @Transactional
    public ProfileImageResponse deleteProfileImage(Long userId) {
        UserProfile profile = getProfile(userId);

        String objectKey = profile.getProfileImageObjectKey();
        if (objectKey != null) {
            fileStorageService.delete(objectKey);
        }
        profile.updateProfileImage(null, null);

        return new ProfileImageResponse(null);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private UserProfile getProfile(Long userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_REQUIRED);
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.IMAGE_SIZE_EXCEEDED);
        }
        String extension = extractExtension(image.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}