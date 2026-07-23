package com.example.v_o_server.domain.answer.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.storage.FileStorageService;
import com.example.v_o_server.domain.answer.dto.request.VideoUploadMetadataRequest;
import com.example.v_o_server.domain.answer.dto.response.VideoResponse;
import com.example.v_o_server.domain.answer.entity.AnswerUploadStatus;
import com.example.v_o_server.domain.answer.entity.DailyAnswer;
import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.answer.entity.VideoStatus;
import com.example.v_o_server.domain.answer.repository.DailyAnswerRepository;
import com.example.v_o_server.domain.answer.repository.VideoRepository;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.service.GroupAccessGuard;
import com.example.v_o_server.domain.question.entity.GroupDailyQuestion;
import com.example.v_o_server.domain.question.entity.Question;
import com.example.v_o_server.domain.question.repository.GroupDailyQuestionRepository;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private static final String STORAGE_DIRECTORY = "videos";
    private static final long MAX_VIDEO_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> SUPPORTED_VIDEO_TYPES = Set.of(
            "video/mp4",
            "video/quicktime",
            "video/webm"
    );

    private final GroupAccessGuard groupAccessGuard;
    private final GroupDailyQuestionRepository groupDailyQuestionRepository;
    private final DailyAnswerRepository dailyAnswerRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public VideoResponse uploadVideo(
            Long userId,
            VideoUploadMetadataRequest metadata,
            MultipartFile video
    ) {
        validateVideo(video);

        Long groupId = metadata.groupId();
        Long questionId = metadata.questionId();
        PrivateGroup group = groupAccessGuard.getActiveGroup(groupId);
        groupAccessGuard.assertMember(groupId, userId);

        LocalDate today = LocalDate.now();
        GroupDailyQuestion groupDailyQuestion = groupDailyQuestionRepository
                .findByGroupIdAndServiceDate(groupId, today)
                .orElseThrow(() -> new BusinessException(ErrorCode.DAILY_QUESTION_NOT_ASSIGNED));

        Question question = groupDailyQuestion.getQuestion();
        if (!question.getId().equals(questionId)) {
            throw new BusinessException(ErrorCode.QUESTION_MISMATCH);
        }

        DailyAnswer dailyAnswer = dailyAnswerRepository
                .findByGroupIdAndUserIdAndServiceDate(groupId, userId, today)
                .orElse(null);
        if (dailyAnswer != null && dailyAnswer.isUploaded()) {
            throw new BusinessException(ErrorCode.ALREADY_UPLOADED_TODAY);
        }

        User user = userRepository.getReferenceById(userId);
        FileStorageService.StoredFile stored = fileStorageService.upload(video, STORAGE_DIRECTORY);
        LocalDateTime now = LocalDateTime.now();

        try {
            if (dailyAnswer == null) {
                dailyAnswer = DailyAnswer.builder()
                        .groupDailyQuestion(groupDailyQuestion)
                        .group(group)
                        .user(user)
                        .question(question)
                        .serviceDate(today)
                        .status(AnswerUploadStatus.NOT_UPLOADED)
                        .build();
            }
            dailyAnswer.markUploaded(now);
            dailyAnswerRepository.save(dailyAnswer);

            Video savedVideo = videoRepository.saveAndFlush(Video.builder()
                    .dailyAnswer(dailyAnswer)
                    .group(group)
                    .user(user)
                    .question(question)
                    .videoUrl(stored.url())
                    .videoObjectKey(stored.objectKey())
                    .mimeType(normalizeContentType(video.getContentType()))
                    .fileSizeBytes(video.getSize())
                    .durationMs(metadata.durationMs())
                    .width(metadata.width())
                    .height(metadata.height())
                    .cameraFacing(metadata.cameraFacing())
                    .status(VideoStatus.ACTIVE)
                    .capturedAt(metadata.capturedAt() == null ? now : metadata.capturedAt())
                    .uploadedAt(now)
                    .build());

            return toResponse(savedVideo);
        } catch (RuntimeException exception) {
            deleteStoredFileAfterFailure(stored.objectKey(), exception);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public VideoResponse getVideo(Long userId, Long videoId) {
        Video video = videoRepository.findByIdAndStatus(videoId, VideoStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
        groupAccessGuard.assertMember(video.getGroup().getId(), userId);
        return toResponse(video);
    }

    private void validateVideo(MultipartFile video) {
        if (video == null || video.isEmpty()) {
            throw new BusinessException(ErrorCode.VIDEO_REQUIRED);
        }
        if (video.getSize() > MAX_VIDEO_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.VIDEO_SIZE_EXCEEDED);
        }
        if (!SUPPORTED_VIDEO_TYPES.contains(normalizeContentType(video.getContentType()))) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_VIDEO_TYPE);
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    }

    private void deleteStoredFileAfterFailure(String objectKey, RuntimeException originalException) {
        try {
            fileStorageService.delete(objectKey);
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
            log.warn("영상 저장 실패 후 파일 정리에도 실패했습니다. objectKey={}", objectKey, cleanupException);
        }
    }

    private VideoResponse toResponse(Video video) {
        return new VideoResponse(
                video.getId(),
                video.getGroup().getId(),
                video.getQuestion().getId(),
                video.getVideoUrl(),
                video.getThumbnailUrl(),
                video.getDurationMs(),
                video.getUploadedAt()
        );
    }
}
