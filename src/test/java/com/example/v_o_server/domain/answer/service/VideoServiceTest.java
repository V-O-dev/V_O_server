package com.example.v_o_server.domain.answer.service;

import static com.example.v_o_server.domain.group.GroupTestFixtures.group;
import static com.example.v_o_server.domain.group.GroupTestFixtures.user;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.storage.FileStorageService;
import com.example.v_o_server.domain.answer.entity.DailyAnswer;
import com.example.v_o_server.domain.answer.entity.Video;
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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("VideoService")
class VideoServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long GROUP_ID = 100L;
    private static final Long QUESTION_ID = 5L;

    @Mock private GroupAccessGuard groupAccessGuard;
    @Mock private GroupDailyQuestionRepository groupDailyQuestionRepository;
    @Mock private DailyAnswerRepository dailyAnswerRepository;
    @Mock private VideoRepository videoRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private ArchiveEntryWriter archiveEntryWriter;

    @InjectMocks private VideoService videoService;

    private final MockMultipartFile videoFile =
            new MockMultipartFile("video", "v.mp4", "video/mp4", new byte[]{1, 2, 3});

    private GroupDailyQuestion gdqReturning(Question question) {
        GroupDailyQuestion gdq = mock(GroupDailyQuestion.class);
        given(gdq.getQuestion()).willReturn(question);
        return gdq;
    }

    private Question questionWithId(Long id) {
        Question question = mock(Question.class);
        given(question.getId()).willReturn(id);
        return question;
    }

    @Test
    @DisplayName("업로드에 성공하면 영상 저장 후 같은 흐름에서 아카이브 기록을 만든다")
    void createsArchiveEntryOnUpload() {
        PrivateGroup groupEntity = group(GROUP_ID, user(USER_ID), 15);
        Question question = questionWithId(QUESTION_ID);
        GroupDailyQuestion gdq = gdqReturning(question);
        User userRef = user(USER_ID);
        Video savedVideo = mock(Video.class);
        given(savedVideo.getId()).willReturn(999L);
        given(savedVideo.getGroup()).willReturn(groupEntity);
        given(savedVideo.getQuestion()).willReturn(question);

        given(groupAccessGuard.getActiveGroup(GROUP_ID)).willReturn(groupEntity);
        given(groupDailyQuestionRepository.findByGroupIdAndServiceDate(eq(GROUP_ID), any(LocalDate.class)))
                .willReturn(Optional.of(gdq));
        given(dailyAnswerRepository.findByGroupIdAndUserIdAndServiceDate(eq(GROUP_ID), eq(USER_ID), any()))
                .willReturn(Optional.empty());
        given(userRepository.getReferenceById(USER_ID)).willReturn(userRef);
        given(fileStorageService.upload(eq(videoFile), eq("videos")))
                .willReturn(new FileStorageService.StoredFile("https://cdn/v.mp4", "videos/v.mp4"));
        given(videoRepository.save(any(Video.class))).willReturn(savedVideo);

        videoService.uploadVideo(USER_ID, GROUP_ID, QUESTION_ID, videoFile);

        verify(videoRepository).save(any(Video.class));
        // 저장된 그 영상으로, 오늘 날짜로 기록이 생성돼야 한다.
        verify(archiveEntryWriter).record(eq(savedVideo), eq(gdq), eq(LocalDate.now()));
    }

    @Test
    @DisplayName("오늘 이미 업로드했으면 ALREADY_UPLOADED_TODAY — 영상·기록을 만들지 않는다")
    void doesNotRecordWhenAlreadyUploaded() {
        PrivateGroup groupEntity = group(GROUP_ID, user(USER_ID), 15);
        Question question = questionWithId(QUESTION_ID);
        GroupDailyQuestion gdq = gdqReturning(question);
        DailyAnswer existing = mock(DailyAnswer.class);
        given(existing.isUploaded()).willReturn(true);

        given(groupAccessGuard.getActiveGroup(GROUP_ID)).willReturn(groupEntity);
        given(groupDailyQuestionRepository.findByGroupIdAndServiceDate(eq(GROUP_ID), any(LocalDate.class)))
                .willReturn(Optional.of(gdq));
        given(dailyAnswerRepository.findByGroupIdAndUserIdAndServiceDate(eq(GROUP_ID), eq(USER_ID), any()))
                .willReturn(Optional.of(existing));

        assertThatThrownBy(() -> videoService.uploadVideo(USER_ID, GROUP_ID, QUESTION_ID, videoFile))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_UPLOADED_TODAY);

        verify(videoRepository, never()).save(any());
        verify(archiveEntryWriter, never()).record(any(), any(), any());
    }

    @Test
    @DisplayName("요청 질문이 오늘 배정 질문과 다르면 QUESTION_MISMATCH — 기록을 만들지 않는다")
    void doesNotRecordOnQuestionMismatch() {
        PrivateGroup groupEntity = group(GROUP_ID, user(USER_ID), 15);
        Question question = questionWithId(QUESTION_ID);
        GroupDailyQuestion gdq = gdqReturning(question);

        given(groupAccessGuard.getActiveGroup(GROUP_ID)).willReturn(groupEntity);
        given(groupDailyQuestionRepository.findByGroupIdAndServiceDate(eq(GROUP_ID), any(LocalDate.class)))
                .willReturn(Optional.of(gdq));

        assertThatThrownBy(() -> videoService.uploadVideo(USER_ID, GROUP_ID, 999L, videoFile))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUESTION_MISMATCH);

        verify(archiveEntryWriter, never()).record(any(), any(), any());
    }

    @Test
    @DisplayName("오늘 배정된 질문이 없으면 DAILY_QUESTION_NOT_ASSIGNED — 기록을 만들지 않는다")
    void doesNotRecordWhenNoDailyQuestion() {
        PrivateGroup groupEntity = group(GROUP_ID, user(USER_ID), 15);
        given(groupAccessGuard.getActiveGroup(GROUP_ID)).willReturn(groupEntity);
        given(groupDailyQuestionRepository.findByGroupIdAndServiceDate(eq(GROUP_ID), any(LocalDate.class)))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> videoService.uploadVideo(USER_ID, GROUP_ID, QUESTION_ID, videoFile))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DAILY_QUESTION_NOT_ASSIGNED);

        verify(archiveEntryWriter, never()).record(any(), any(), any());
    }
}
