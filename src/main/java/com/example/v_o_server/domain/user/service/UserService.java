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
import com.example.v_o_server.domain.user.dto.response.ProfileCreateResponse;
import com.example.v_o_server.domain.user.dto.response.ProfileImageResponse;
import com.example.v_o_server.domain.user.dto.response.UserMeResponse;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.entity.UserProfile;
import com.example.v_o_server.domain.user.repository.UserProfileRepository;
import com.example.v_o_server.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
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
    private static final int MAX_NICKNAME_LENGTH = 15;
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]*$");

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

    /**
     * 온보딩에서 입력한 이름·사진으로 프로필을 생성한다.
     * 사진은 선택 사항이며, 첨부하지 않으면 이미지 없이 생성된다.
     */
    @Transactional
    public ProfileCreateResponse createProfile(Long userId, String nickname, MultipartFile image) {
        if (userProfileRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.PROFILE_ALREADY_EXISTS);
        }

        String validatedNickname = validateNickname(nickname);
        User user = getUser(userId);

        // 사진 미첨부는 정상 흐름(건너뛰기)이므로 검증/업로드를 건너뛴다.
        StoredFile stored = null;
        if (image != null && !image.isEmpty()) {
            validateImage(image);
            stored = fileStorageService.upload(image, PROFILE_IMAGE_DIR);
        }

        UserProfile profile = UserProfile.builder()
                .user(user)
                .nickname(validatedNickname)
                .profileImageUrl(stored == null ? null : stored.url())
                .profileImageObjectKey(stored == null ? null : stored.objectKey())
                .build();
        profile.completeOnboarding(LocalDateTime.now());

        return ProfileCreateResponse.from(userProfileRepository.save(profile));
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

    /**
     * multipart 폼 필드로 들어오는 닉네임은 DTO @Valid가 걸리지 않아 여기서 검증한다.
     * 전용 에러 코드(U003~U005)로 응답해 클라이언트가 사유를 구분할 수 있게 한다.
     */
    private String validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new BusinessException(ErrorCode.NICKNAME_BLANK);
        }
        String trimmed = nickname.strip();
        if (trimmed.length() > MAX_NICKNAME_LENGTH) {
            throw new BusinessException(ErrorCode.NICKNAME_TOO_LONG);
        }
        if (!NICKNAME_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException(ErrorCode.NICKNAME_INVALID_CHAR);
        }
        return trimmed;
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
