package com.example.v_o_server.domain.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.v_o_server.domain.answer.entity.AnswerUploadStatus;
import com.example.v_o_server.domain.answer.entity.DailyAnswer;
import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.answer.entity.VideoStatus;
import com.example.v_o_server.domain.answer.repository.DailyAnswerRepository;
import com.example.v_o_server.domain.answer.repository.VideoRepository;
import com.example.v_o_server.domain.feed.dto.FeedResponse;
import com.example.v_o_server.domain.group.service.GroupAccessGuard;
import com.example.v_o_server.domain.question.entity.Question;
import com.example.v_o_server.domain.question.entity.QuestionStatus;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.entity.UserProfile;
import com.example.v_o_server.domain.user.entity.UserStatus;
import com.example.v_o_server.domain.user.repository.UserProfileRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("FeedService")
class FeedServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long GROUP_ID = 10L;
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 7, 24);

    private GroupAccessGuard groupAccessGuard;
    private DailyAnswerRepository dailyAnswerRepository;
    private VideoRepository videoRepository;
    private UserProfileRepository userProfileRepository;
    private FeedService feedService;

    @BeforeEach
    void setUp() {
        groupAccessGuard = org.mockito.Mockito.mock(GroupAccessGuard.class);
        dailyAnswerRepository = org.mockito.Mockito.mock(DailyAnswerRepository.class);
        videoRepository = org.mockito.Mockito.mock(VideoRepository.class);
        userProfileRepository = org.mockito.Mockito.mock(UserProfileRepository.class);
        feedService = new FeedService(
                groupAccessGuard,
                dailyAnswerRepository,
                videoRepository,
                userProfileRepository
        );
    }

    @Test
    @DisplayName("답변을 업로드하지 않은 멤버에게는 잠긴 빈 피드를 반환한다")
    void returnsLockedFeedWhenViewerHasNoAnswer() {
        given(dailyAnswerRepository.findByGroupIdAndUserIdAndServiceDate(
                GROUP_ID, USER_ID, SERVICE_DATE)).willReturn(Optional.empty());

        FeedResponse response = feedService.getFeed(USER_ID, GROUP_ID, SERVICE_DATE, 0, 20);

        assertThat(response.unlocked()).isFalse();
        assertThat(response.viewerAnswerStatus()).isEqualTo(AnswerUploadStatus.NOT_UPLOADED);
        assertThat(response.items()).isEmpty();
        assertThat(response.totalElements()).isZero();
        verify(groupAccessGuard).getActiveGroup(GROUP_ID);
        verify(groupAccessGuard).assertMember(GROUP_ID, USER_ID);
        verify(videoRepository, never())
                .findByGroupIdAndDailyAnswerServiceDateAndStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("업로드 실패 상태인 멤버에게도 피드를 잠근다")
    void returnsLockedFeedWhenUploadFailed() {
        DailyAnswer failedAnswer = answer(AnswerUploadStatus.UPLOAD_FAILED);
        given(dailyAnswerRepository.findByGroupIdAndUserIdAndServiceDate(
                GROUP_ID, USER_ID, SERVICE_DATE)).willReturn(Optional.of(failedAnswer));

        FeedResponse response = feedService.getFeed(USER_ID, GROUP_ID, SERVICE_DATE, 1, 10);

        assertThat(response.unlocked()).isFalse();
        assertThat(response.viewerAnswerStatus()).isEqualTo(AnswerUploadStatus.UPLOAD_FAILED);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        verify(videoRepository, never())
                .findByGroupIdAndDailyAnswerServiceDateAndStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("업로드를 완료한 멤버에게 ACTIVE 영상 피드와 작성자 정보를 반환한다")
    void returnsUnlockedFeedWithVideoAndAuthor() {
        DailyAnswer uploadedAnswer = answer(AnswerUploadStatus.UPLOADED);
        User author = user(2L);
        Question question = question(30L, "오늘 가장 웃겼던 일은?");
        LocalDateTime uploadedAt = LocalDateTime.of(2026, 7, 24, 12, 0);
        Video video = video(100L, author, question, uploadedAt);
        UserProfile profile = profile(author, "동구", "https://cdn.example.com/profile.jpg");
        PageRequest repositoryPage = PageRequest.of(0, 2);

        given(dailyAnswerRepository.findByGroupIdAndUserIdAndServiceDate(
                GROUP_ID, USER_ID, SERVICE_DATE)).willReturn(Optional.of(uploadedAnswer));
        given(videoRepository.findByGroupIdAndDailyAnswerServiceDateAndStatus(
                org.mockito.ArgumentMatchers.eq(GROUP_ID),
                org.mockito.ArgumentMatchers.eq(SERVICE_DATE),
                org.mockito.ArgumentMatchers.eq(VideoStatus.ACTIVE),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(video), repositoryPage, 3));
        given(userProfileRepository.findAllById(any())).willReturn(List.of(profile));

        FeedResponse response = feedService.getFeed(USER_ID, GROUP_ID, SERVICE_DATE, 0, 2);

        assertThat(response.unlocked()).isTrue();
        assertThat(response.viewerAnswerStatus()).isEqualTo(AnswerUploadStatus.UPLOADED);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.videoId()).isEqualTo(100L);
            assertThat(item.userId()).isEqualTo(2L);
            assertThat(item.nickname()).isEqualTo("동구");
            assertThat(item.profileImageUrl()).isEqualTo("https://cdn.example.com/profile.jpg");
            assertThat(item.questionId()).isEqualTo(30L);
            assertThat(item.questionContent()).isEqualTo("오늘 가장 웃겼던 일은?");
            assertThat(item.videoUrl()).isEqualTo("https://cdn.example.com/video.mp4");
            assertThat(item.uploadedAt()).isEqualTo(uploadedAt);
        });

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(videoRepository).findByGroupIdAndDailyAnswerServiceDateAndStatus(
                org.mockito.ArgumentMatchers.eq(GROUP_ID),
                org.mockito.ArgumentMatchers.eq(SERVICE_DATE),
                org.mockito.ArgumentMatchers.eq(VideoStatus.ACTIVE),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("uploadedAt").isDescending()).isTrue();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
    }

    private DailyAnswer answer(AnswerUploadStatus status) {
        return DailyAnswer.builder()
                .serviceDate(SERVICE_DATE)
                .status(status)
                .build();
    }

    private User user(Long id) {
        User user = User.builder()
                .status(UserStatus.ACTIVE)
                .dailyQuestionNotificationEnabled(true)
                .interactionNotificationEnabled(true)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Question question(Long id, String content) {
        Question question = Question.builder()
                .content(content)
                .status(QuestionStatus.ACTIVE)
                .answerTimeLimitMs(10_000)
                .useCount(0)
                .build();
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private Video video(Long id, User author, Question question, LocalDateTime uploadedAt) {
        Video video = Video.builder()
                .user(author)
                .question(question)
                .videoUrl("https://cdn.example.com/video.mp4")
                .thumbnailUrl("https://cdn.example.com/thumbnail.jpg")
                .durationMs(10_000)
                .status(VideoStatus.ACTIVE)
                .capturedAt(uploadedAt.minusSeconds(10))
                .uploadedAt(uploadedAt)
                .build();
        ReflectionTestUtils.setField(video, "id", id);
        return video;
    }

    private UserProfile profile(User user, String nickname, String profileImageUrl) {
        UserProfile profile = UserProfile.builder()
                .user(user)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .build();
        ReflectionTestUtils.setField(profile, "id", user.getId());
        return profile;
    }
}
