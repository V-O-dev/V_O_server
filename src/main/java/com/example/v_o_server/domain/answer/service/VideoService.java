package com.example.v_o_server.domain.answer.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.storage.FileStorageService;
import com.example.v_o_server.domain.answer.dto.response.VideoResponse;
import com.example.v_o_server.domain.answer.entity.AnswerUploadStatus;
import com.example.v_o_server.domain.answer.entity.DailyAnswer;
import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.answer.entity.VideoStatus;
import com.example.v_o_server.domain.answer.repository.DailyAnswerRepository;
import com.example.v_o_server.domain.answer.repository.VideoRepository;
import com.example.v_o_server.domain.archive.service.ArchiveEntryWriter;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.service.GroupAccessGuard;
import com.example.v_o_server.domain.question.entity.GroupDailyQuestion;
import com.example.v_o_server.domain.question.entity.Question;
import com.example.v_o_server.domain.question.repository.GroupDailyQuestionRepository;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class VideoService {

    private static final String STORAGE_DIRECTORY = "videos";

    private final GroupAccessGuard groupAccessGuard;
    private final GroupDailyQuestionRepository groupDailyQuestionRepository;
    private final DailyAnswerRepository dailyAnswerRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ArchiveEntryWriter archiveEntryWriter;

    @Transactional
    public VideoResponse uploadVideo(Long userId, Long groupId, Long questionId, MultipartFile video) {
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

        Video savedVideo = videoRepository.save(Video.builder()
                .dailyAnswer(dailyAnswer)
                .group(group)
                .user(user)
                .question(question)
                .videoUrl(stored.url())
                .videoObjectKey(stored.objectKey())
                .mimeType(video.getContentType())
                .fileSizeBytes(video.getSize())
                .status(VideoStatus.ACTIVE)
                .capturedAt(now)
                .uploadedAt(now)
                .build());

        // 같은 트랜잭션에서 나의 달력 기록을 만든다. 영상 저장 뒤라야 video_id FK를 채울 수 있다.
        archiveEntryWriter.record(savedVideo, groupDailyQuestion, today);

        return toResponse(savedVideo);
    }

    public VideoResponse getVideo(Long userId, Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
        groupAccessGuard.assertMember(video.getGroup().getId(), userId);
        return toResponse(video);
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
