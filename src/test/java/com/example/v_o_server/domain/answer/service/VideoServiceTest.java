package com.example.v_o_server.domain.answer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
import com.example.v_o_server.domain.archive.service.ArchiveEntryWriter;
import com.example.v_o_server.domain.group.entity.GroupStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.service.GroupAccessGuard;
import com.example.v_o_server.domain.question.entity.AssignmentStatus;
import com.example.v_o_server.domain.question.entity.GroupDailyQuestion;
import com.example.v_o_server.domain.question.entity.Question;
import com.example.v_o_server.domain.question.entity.QuestionStatus;
import com.example.v_o_server.domain.question.repository.GroupDailyQuestionRepository;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.entity.UserStatus;
import com.example.v_o_server.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("VideoService")
class VideoServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long GROUP_ID = 10L;
    private static final Long QUESTION_ID = 20L;
    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
    // UTC 2026-07-24 15:30은 KST 2026-07-25 00:30이다. 두 날짜가 갈리는 경계를 고정한다.
    private static final Clock FIXED_KST_CLOCK = Clock.fixed(
            Instant.parse("2026-07-24T15:30:00Z"),
            KOREA_ZONE_ID
    );
    private static final LocalDate KST_TODAY = LocalDate.of(2026, 7, 25);
    private static final LocalDateTime KST_NOW = LocalDateTime.of(2026, 7, 25, 0, 30);

    private GroupAccessGuard groupAccessGuard;
    private GroupDailyQuestionRepository groupDailyQuestionRepository;
    private DailyAnswerRepository dailyAnswerRepository;
    private VideoRepository videoRepository;
    private UserRepository userRepository;
    private FileStorageService fileStorageService;
    private ArchiveEntryWriter archiveEntryWriter;
    private VideoService videoService;

    @BeforeEach
    void setUp() {
        groupAccessGuard = org.mockito.Mockito.mock(GroupAccessGuard.class);
        groupDailyQuestionRepository = org.mockito.Mockito.mock(GroupDailyQuestionRepository.class);
        dailyAnswerRepository = org.mockito.Mockito.mock(DailyAnswerRepository.class);
        videoRepository = org.mockito.Mockito.mock(VideoRepository.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        fileStorageService = org.mockito.Mockito.mock(FileStorageService.class);
        archiveEntryWriter = org.mockito.Mockito.mock(ArchiveEntryWriter.class);
        videoService = new VideoService(
                groupAccessGuard,
                groupDailyQuestionRepository,
                dailyAnswerRepository,
                videoRepository,
                userRepository,
                fileStorageService,
                archiveEntryWriter,
                FIXED_KST_CLOCK
        );
    }

    @Test
    @DisplayName("영상 업로드는 답변 상태와 영상 메타데이터를 저장하고 아카이브 기록을 남긴다")
    void uploadsVideoAndMarksDailyAnswerUploaded() {
        UploadContext context = givenUploadContext();
        MultipartFile videoFile = videoFile("video/mp4", 1024, false);
        VideoUploadMetadataRequest metadata = metadata(
                QUESTION_ID,
                10_000,
                1080,
                1920,
                "FRONT",
                LocalDateTime.of(2026, 7, 24, 12, 0)
        );
        given(fileStorageService.upload(videoFile, "videos"))
                .willReturn(new FileStorageService.StoredFile(
                        "https://cdn.example.com/video.mp4",
                        "videos/video.mp4"
                ));
        given(videoRepository.saveAndFlush(any(Video.class))).willAnswer(invocation -> {
            Video saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        VideoResponse response = videoService.uploadVideo(USER_ID, metadata, videoFile);

        assertThat(response.videoId()).isEqualTo(100L);
        assertThat(response.groupId()).isEqualTo(GROUP_ID);
        assertThat(response.questionId()).isEqualTo(QUESTION_ID);
        assertThat(response.videoUrl()).isEqualTo("https://cdn.example.com/video.mp4");
        assertThat(response.durationMs()).isEqualTo(10_000);

        ArgumentCaptor<DailyAnswer> answerCaptor = ArgumentCaptor.forClass(DailyAnswer.class);
        verify(dailyAnswerRepository).save(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getStatus()).isEqualTo(AnswerUploadStatus.UPLOADED);

        ArgumentCaptor<Video> videoCaptor = ArgumentCaptor.forClass(Video.class);
        verify(videoRepository).saveAndFlush(videoCaptor.capture());
        assertThat(videoCaptor.getValue().getDailyAnswer()).isSameAs(answerCaptor.getValue());
        assertThat(videoCaptor.getValue().getWidth()).isEqualTo(1080);
        assertThat(videoCaptor.getValue().getHeight()).isEqualTo(1920);
        assertThat(videoCaptor.getValue().getCameraFacing()).isEqualTo("FRONT");
        assertThat(videoCaptor.getValue().getCapturedAt()).isEqualTo(metadata.capturedAt());
        assertThat(videoCaptor.getValue().getUploadedAt()).isEqualTo(KST_NOW);
        assertThat(videoCaptor.getValue().getMimeType()).isEqualTo("video/mp4");
        assertThat(context.groupDailyQuestion().getQuestion()).isSameAs(context.question());
        assertThat(answerCaptor.getValue().getServiceDate()).isEqualTo(KST_TODAY);

        // 서버 기본 타임존과 무관하게 KST 업로드 날짜로 아카이브 기록이 생성돼야 한다.
        verify(archiveEntryWriter)
                .record(eq(videoCaptor.getValue()), eq(context.groupDailyQuestion()), eq(KST_TODAY));
    }

    @Test
    @DisplayName("빈 영상은 V010으로 거부하고 도메인 조회를 하지 않는다")
    void rejectsEmptyVideo() {
        MultipartFile emptyVideo = videoFile("video/mp4", 0, true);

        BusinessException exception = catchThrowableOfType(
                () -> videoService.uploadVideo(USER_ID, metadata(), emptyVideo),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VIDEO_REQUIRED);
        verifyNoInteractions(groupAccessGuard, fileStorageService, videoRepository, archiveEntryWriter);
    }

    @Test
    @DisplayName("지원하지 않는 파일 형식은 V011로 거부한다")
    void rejectsUnsupportedContentType() {
        MultipartFile textFile = videoFile("text/plain", 100, false);

        BusinessException exception = catchThrowableOfType(
                () -> videoService.uploadVideo(USER_ID, metadata(), textFile),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_VIDEO_TYPE);
        verifyNoInteractions(groupAccessGuard, fileStorageService, videoRepository, archiveEntryWriter);
    }

    @Test
    @DisplayName("10MB를 넘는 영상은 V012로 거부한다")
    void rejectsOversizedVideo() {
        MultipartFile oversizedVideo = videoFile("video/mp4", 10L * 1024 * 1024 + 1, false);

        BusinessException exception = catchThrowableOfType(
                () -> videoService.uploadVideo(USER_ID, metadata(), oversizedVideo),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VIDEO_SIZE_EXCEEDED);
        verifyNoInteractions(groupAccessGuard, fileStorageService, videoRepository, archiveEntryWriter);
    }

    @Test
    @DisplayName("오늘 배정된 질문이 없으면 V007로 거부하고 기록하지 않는다")
    void rejectsWhenNoDailyQuestionAssigned() {
        UploadContext context = uploadContext();
        given(groupAccessGuard.getActiveGroup(GROUP_ID)).willReturn(context.group());
        given(groupDailyQuestionRepository.findByGroupIdAndServiceDate(GROUP_ID, KST_TODAY))
                .willReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> videoService.uploadVideo(
                        USER_ID,
                        metadata(),
                        videoFile("video/mp4", 1024, false)
                ),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DAILY_QUESTION_NOT_ASSIGNED);
        verifyNoInteractions(fileStorageService);
        verify(archiveEntryWriter, never()).record(any(), any(), any());
    }

    @Test
    @DisplayName("오늘 질문과 다른 질문 ID는 V008으로 거부하고 기록하지 않는다")
    void rejectsMismatchedQuestion() {
        givenUploadContext();
        MultipartFile videoFile = videoFile("video/mp4", 1024, false);

        BusinessException exception = catchThrowableOfType(
                () -> videoService.uploadVideo(
                        USER_ID,
                        metadata(999L, null, null, null, null, null),
                        videoFile
                ),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.QUESTION_MISMATCH);
        verifyNoInteractions(fileStorageService);
        verify(archiveEntryWriter, never()).record(any(), any(), any());
    }

    @Test
    @DisplayName("오늘 이미 업로드한 답변이 있으면 V009로 거부하고 기록하지 않는다")
    void rejectsDuplicateUpload() {
        UploadContext context = givenUploadContext();
        DailyAnswer uploadedAnswer = DailyAnswer.builder()
                .groupDailyQuestion(context.groupDailyQuestion())
                .group(context.group())
                .user(context.user())
                .question(context.question())
                .serviceDate(KST_TODAY)
                .status(AnswerUploadStatus.UPLOADED)
                .build();
        given(dailyAnswerRepository.findByGroupIdAndUserIdAndServiceDate(
                GROUP_ID, USER_ID, KST_TODAY)).willReturn(Optional.of(uploadedAnswer));

        BusinessException exception = catchThrowableOfType(
                () -> videoService.uploadVideo(
                        USER_ID,
                        metadata(),
                        videoFile("video/mp4", 1024, false)
                ),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_UPLOADED_TODAY);
        verifyNoInteractions(fileStorageService);
        verify(archiveEntryWriter, never()).record(any(), any(), any());
    }

    @Test
    @DisplayName("DB 저장 실패 시 먼저 저장한 영상 파일을 보상 삭제하고 기록하지 않는다")
    void deletesStoredFileWhenDatabaseSaveFails() {
        givenUploadContext();
        MultipartFile videoFile = videoFile("video/mp4", 1024, false);
        given(fileStorageService.upload(videoFile, "videos"))
                .willReturn(new FileStorageService.StoredFile(
                        "https://cdn.example.com/video.mp4",
                        "videos/orphan.mp4"
                ));
        given(videoRepository.saveAndFlush(any(Video.class)))
                .willThrow(new IllegalStateException("DB save failed"));

        IllegalStateException exception = catchThrowableOfType(
                () -> videoService.uploadVideo(USER_ID, metadata(), videoFile),
                IllegalStateException.class
        );

        assertThat(exception).hasMessage("DB save failed");
        verify(fileStorageService).delete("videos/orphan.mp4");
        verify(archiveEntryWriter, never()).record(any(), any(), any());
    }

    @Test
    @DisplayName("영상 상세 조회는 ACTIVE 상태만 조회하고 그룹 멤버십을 확인한다")
    void getsOnlyActiveVideoForGroupMember() {
        UploadContext context = uploadContext();
        Video activeVideo = Video.builder()
                .group(context.group())
                .question(context.question())
                .videoUrl("https://cdn.example.com/video.mp4")
                .durationMs(10_000)
                .status(VideoStatus.ACTIVE)
                .uploadedAt(LocalDateTime.of(2026, 7, 24, 12, 0))
                .build();
        ReflectionTestUtils.setField(activeVideo, "id", 100L);
        given(videoRepository.findByIdAndStatus(100L, VideoStatus.ACTIVE))
                .willReturn(Optional.of(activeVideo));

        VideoResponse response = videoService.getVideo(USER_ID, 100L);

        assertThat(response.videoId()).isEqualTo(100L);
        verify(groupAccessGuard).assertMember(GROUP_ID, USER_ID);
        verify(videoRepository, never()).findById(100L);
    }

    @Test
    @DisplayName("ACTIVE 상태가 아닌 영상은 V001로 응답한다")
    void hidesNonActiveVideo() {
        given(videoRepository.findByIdAndStatus(100L, VideoStatus.ACTIVE))
                .willReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> videoService.getVideo(USER_ID, 100L),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VIDEO_NOT_FOUND);
        verifyNoInteractions(groupAccessGuard);
    }

    private UploadContext givenUploadContext() {
        UploadContext context = uploadContext();
        given(groupAccessGuard.getActiveGroup(GROUP_ID)).willReturn(context.group());
        given(groupDailyQuestionRepository.findByGroupIdAndServiceDate(GROUP_ID, KST_TODAY))
                .willReturn(Optional.of(context.groupDailyQuestion()));
        given(dailyAnswerRepository.findByGroupIdAndUserIdAndServiceDate(
                GROUP_ID, USER_ID, KST_TODAY)).willReturn(Optional.empty());
        given(userRepository.getReferenceById(USER_ID)).willReturn(context.user());
        return context;
    }

    private UploadContext uploadContext() {
        User user = User.builder()
                .status(UserStatus.ACTIVE)
                .dailyQuestionNotificationEnabled(true)
                .interactionNotificationEnabled(true)
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);

        PrivateGroup group = PrivateGroup.builder()
                .owner(user)
                .name("우리 가족")
                .status(GroupStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(group, "id", GROUP_ID);

        Question question = Question.builder()
                .content("오늘 가장 웃겼던 일은?")
                .status(QuestionStatus.ACTIVE)
                .answerTimeLimitMs(10_000)
                .useCount(0)
                .build();
        ReflectionTestUtils.setField(question, "id", QUESTION_ID);

        GroupDailyQuestion groupDailyQuestion = GroupDailyQuestion.builder()
                .group(group)
                .question(question)
                .serviceDate(KST_TODAY)
                .questionContentSnapshot(question.getContent())
                .answerTimeLimitMs(10_000)
                .status(AssignmentStatus.ACTIVE)
                .build();
        return new UploadContext(user, group, question, groupDailyQuestion);
    }

    private VideoUploadMetadataRequest metadata() {
        return metadata(QUESTION_ID, null, null, null, null, null);
    }

    private VideoUploadMetadataRequest metadata(
            Long questionId,
            Integer durationMs,
            Integer width,
            Integer height,
            String cameraFacing,
            LocalDateTime capturedAt
    ) {
        return new VideoUploadMetadataRequest(
                GROUP_ID,
                questionId,
                durationMs,
                width,
                height,
                cameraFacing,
                capturedAt
        );
    }

    private MultipartFile videoFile(String contentType, long size, boolean empty) {
        MultipartFile video = org.mockito.Mockito.mock(MultipartFile.class);
        given(video.getContentType()).willReturn(contentType);
        given(video.getSize()).willReturn(size);
        given(video.isEmpty()).willReturn(empty);
        return video;
    }

    private record UploadContext(
            User user,
            PrivateGroup group,
            Question question,
            GroupDailyQuestion groupDailyQuestion
    ) {
    }
}
