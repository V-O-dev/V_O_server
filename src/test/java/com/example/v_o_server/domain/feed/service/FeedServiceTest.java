package com.example.v_o_server.domain.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.v_o_server.domain.answer.entity.AnswerUploadStatus;
import com.example.v_o_server.domain.answer.entity.DailyAnswer;
import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.answer.entity.VideoStatus;
import com.example.v_o_server.domain.answer.repository.DailyAnswerRepository;
import com.example.v_o_server.domain.answer.repository.VideoRepository;
import com.example.v_o_server.domain.feed.dto.FeedResponse;
import com.example.v_o_server.domain.feed.repository.FeedCommentQueryRepository;
import com.example.v_o_server.domain.feed.repository.FeedCountProjection;
import com.example.v_o_server.domain.feed.repository.FeedReactionQueryRepository;
import com.example.v_o_server.domain.group.service.GroupAccessGuard;
import com.example.v_o_server.domain.group.service.GroupMemberAliasReader;
import com.example.v_o_server.domain.group.service.GroupMemberIdResolver;
import com.example.v_o_server.domain.question.entity.Question;
import com.example.v_o_server.domain.question.entity.QuestionStatus;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.entity.UserProfile;
import com.example.v_o_server.domain.user.entity.UserStatus;
import com.example.v_o_server.domain.user.repository.UserProfileRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private FeedReactionQueryRepository feedReactionQueryRepository;
    private FeedCommentQueryRepository feedCommentQueryRepository;
    private GroupMemberAliasReader aliasReader;
    private GroupMemberIdResolver memberIdResolver;
    private FeedService feedService;

    @BeforeEach
    void setUp() {
        groupAccessGuard = org.mockito.Mockito.mock(GroupAccessGuard.class);
        dailyAnswerRepository = org.mockito.Mockito.mock(DailyAnswerRepository.class);
        videoRepository = org.mockito.Mockito.mock(VideoRepository.class);
        userProfileRepository = org.mockito.Mockito.mock(UserProfileRepository.class);
        feedReactionQueryRepository = org.mockito.Mockito.mock(FeedReactionQueryRepository.class);
        feedCommentQueryRepository = org.mockito.Mockito.mock(FeedCommentQueryRepository.class);
        aliasReader = org.mockito.Mockito.mock(GroupMemberAliasReader.class);
        given(aliasReader.findAliases(any(), any(), any())).willReturn(Map.of());
        memberIdResolver = org.mockito.Mockito.mock(GroupMemberIdResolver.class);
        given(memberIdResolver.findMemberIds(any(), any())).willReturn(Map.of());
        feedService = new FeedService(
                groupAccessGuard,
                dailyAnswerRepository,
                videoRepository,
                userProfileRepository,
                feedReactionQueryRepository,
                feedCommentQueryRepository,
                aliasReader,
                memberIdResolver
        );
    }

    @Test
    @DisplayName("답변을 업로드하지 않은 멤버에게 잠긴 상태와 다른 멤버의 피드를 반환한다")
    void returnsLockedFeedWhenViewerHasNoAnswer() {
        User author = user(2L);
        Video video = video(100L, author, question(30L, "오늘 가장 웃겼던 일은?"),
                LocalDateTime.of(2026, 7, 24, 12, 0));
        given(dailyAnswerRepository.findByGroupIdAndUserIdAndServiceDate(
                GROUP_ID, USER_ID, SERVICE_DATE)).willReturn(Optional.empty());
        given(videoRepository.findByGroupIdAndDailyAnswerServiceDateAndStatus(
                eq(GROUP_ID), eq(SERVICE_DATE), eq(VideoStatus.ACTIVE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(video), PageRequest.of(0, 20), 1));
        given(userProfileRepository.findAllById(any())).willReturn(List.of());
        given(feedReactionQueryRepository.countByVideoIds(List.of(100L))).willReturn(List.of());
        given(feedReactionQueryRepository.findReactedVideoIds(USER_ID, List.of(100L))).willReturn(List.of());
        given(feedCommentQueryRepository.countActiveByVideoIds(List.of(100L))).willReturn(List.of());

        FeedResponse response = feedService.getFeed(USER_ID, GROUP_ID, SERVICE_DATE, 0, 20);

        assertThat(response.unlocked()).isFalse();
        assertThat(response.viewerAnswerStatus()).isEqualTo(AnswerUploadStatus.NOT_UPLOADED);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.videoId()).isEqualTo(100L);
            assertThat(item.userId()).isEqualTo(2L);
            assertThat(item.videoUrl()).isEqualTo("https://cdn.example.com/video.mp4");
        });
        assertThat(response.totalElements()).isOne();
        verify(groupAccessGuard).getActiveGroup(GROUP_ID);
        verify(groupAccessGuard).assertMember(GROUP_ID, USER_ID);
        verify(videoRepository).findByGroupIdAndDailyAnswerServiceDateAndStatus(
                eq(GROUP_ID), eq(SERVICE_DATE), eq(VideoStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    @DisplayName("업로드 실패 상태인 멤버에게도 잠긴 상태와 다른 멤버의 피드를 반환한다")
    void returnsLockedFeedWhenUploadFailed() {
        DailyAnswer failedAnswer = answer(AnswerUploadStatus.UPLOAD_FAILED);
        User author = user(2L);
        Video video = video(101L, author, question(31L, "오늘 고마웠던 일은?"),
                LocalDateTime.of(2026, 7, 24, 13, 0));
        given(dailyAnswerRepository.findByGroupIdAndUserIdAndServiceDate(
                GROUP_ID, USER_ID, SERVICE_DATE)).willReturn(Optional.of(failedAnswer));
        given(videoRepository.findByGroupIdAndDailyAnswerServiceDateAndStatus(
                eq(GROUP_ID), eq(SERVICE_DATE), eq(VideoStatus.ACTIVE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(video), PageRequest.of(1, 10), 11));
        given(userProfileRepository.findAllById(any())).willReturn(List.of());
        given(feedReactionQueryRepository.countByVideoIds(List.of(101L))).willReturn(List.of());
        given(feedReactionQueryRepository.findReactedVideoIds(USER_ID, List.of(101L))).willReturn(List.of());
        given(feedCommentQueryRepository.countActiveByVideoIds(List.of(101L))).willReturn(List.of());

        FeedResponse response = feedService.getFeed(USER_ID, GROUP_ID, SERVICE_DATE, 1, 10);

        assertThat(response.unlocked()).isFalse();
        assertThat(response.viewerAnswerStatus()).isEqualTo(AnswerUploadStatus.UPLOAD_FAILED);
        assertThat(response.items()).singleElement()
                .satisfies(item -> assertThat(item.videoId()).isEqualTo(101L));
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(11);
        assertThat(response.hasNext()).isFalse();
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
        given(feedReactionQueryRepository.countByVideoIds(List.of(100L)))
                .willReturn(List.of(count(100L, 4L)));
        given(feedReactionQueryRepository.findReactedVideoIds(USER_ID, List.of(100L)))
                .willReturn(List.of(100L));
        given(feedCommentQueryRepository.countActiveByVideoIds(List.of(100L)))
                .willReturn(List.of(count(100L, 2L)));

        FeedResponse response = feedService.getFeed(USER_ID, GROUP_ID, SERVICE_DATE, 0, 2);

        assertThat(response.unlocked()).isTrue();
        assertThat(response.viewerAnswerStatus()).isEqualTo(AnswerUploadStatus.UPLOADED);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.videoId()).isEqualTo(100L);
            assertThat(item.userId()).isEqualTo(2L);
            assertThat(item.nickname()).isEqualTo("동구");
            // 호칭을 지정하지 않았으므로 alias는 비고 표시 이름은 전역 닉네임 그대로.
            assertThat(item.alias()).isNull();
            assertThat(item.displayName()).isEqualTo("동구");
            assertThat(item.profileImageUrl()).isEqualTo("https://cdn.example.com/profile.jpg");
            assertThat(item.questionId()).isEqualTo(30L);
            assertThat(item.questionContent()).isEqualTo("오늘 가장 웃겼던 일은?");
            assertThat(item.videoUrl()).isEqualTo("https://cdn.example.com/video.mp4");
            assertThat(item.reactionCount()).isEqualTo(4);
            assertThat(item.reactedByMe()).isTrue();
            assertThat(item.commentCount()).isEqualTo(2);
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

    @Test
    @DisplayName("내가 지정한 호칭이 있으면 피드 카드 이름이 호칭으로 나온다 (닉네임은 그대로 유지)")
    void appliesViewerAliasToFeedItem() {
        User author = givenSingleVideoFeed(profile(user(2L), "동구", "https://cdn.example.com/profile.jpg"));
        given(aliasReader.findAliases(eq(GROUP_ID), eq(USER_ID), any())).willReturn(Map.of(2L, "막내"));

        FeedResponse response = feedService.getFeed(USER_ID, GROUP_ID, SERVICE_DATE, 0, 2);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.displayName()).isEqualTo("막내");
            // 이름 편집 화면 입력 기본값으로 쓰도록 호칭 원본도 함께 내려준다.
            assertThat(item.alias()).isEqualTo("막내");
            assertThat(item.nickname()).isEqualTo("동구");
            assertThat(item.userId()).isEqualTo(author.getId());
        });
    }

    @Test
    @DisplayName("호칭 조회는 이 그룹과 조회자 기준으로, 작성자 userId 집합으로 좁혀 호출된다")
    void aliasLookupIsScopedToViewerGroupAndAuthors() {
        givenSingleVideoFeed(profile(user(2L), "동구", null));
        given(aliasReader.findAliases(any(), any(), any())).willReturn(Map.of());

        feedService.getFeed(USER_ID, GROUP_ID, SERVICE_DATE, 0, 2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<Long>> captor =
                ArgumentCaptor.forClass(java.util.Collection.class);
        verify(aliasReader).findAliases(eq(GROUP_ID), eq(USER_ID), captor.capture());
        assertThat(captor.getValue()).containsExactly(2L);
    }

    @Test
    @DisplayName("피드 카드에 작성자의 memberId를 실어 호칭 편집 화면으로 바로 갈 수 있게 한다")
    void carriesMemberIdSoClientCanOpenAliasEditor() {
        givenSingleVideoFeed(profile(user(2L), "동구", null));
        given(memberIdResolver.findMemberIds(eq(GROUP_ID), any())).willReturn(Map.of(2L, 21L));

        FeedResponse response = feedService.getFeed(USER_ID, GROUP_ID, SERVICE_DATE, 0, 2);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.memberId()).isEqualTo(21L);
            assertThat(item.isMe()).isFalse();
        });
        verify(memberIdResolver).findMemberIds(eq(GROUP_ID), any());
    }

    @Test
    @DisplayName("내 영상에는 memberId를 내리지 않는다 — 자기 자신에게는 호칭을 지정할 수 없다")
    void memberIdIsNullForMyOwnVideo() {
        User me = user(USER_ID);
        Video myVideo = video(100L, me, question(30L, "오늘 가장 웃겼던 일은?"),
                LocalDateTime.of(2026, 7, 24, 12, 0));

        given(dailyAnswerRepository.findByGroupIdAndUserIdAndServiceDate(GROUP_ID, USER_ID, SERVICE_DATE))
                .willReturn(Optional.of(answer(AnswerUploadStatus.UPLOADED)));
        given(videoRepository.findByGroupIdAndDailyAnswerServiceDateAndStatus(
                eq(GROUP_ID), eq(SERVICE_DATE), eq(VideoStatus.ACTIVE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(myVideo), PageRequest.of(0, 2), 1));
        given(userProfileRepository.findAllById(any())).willReturn(List.of());
        given(feedReactionQueryRepository.countByVideoIds(List.of(100L))).willReturn(List.of());
        given(feedReactionQueryRepository.findReactedVideoIds(USER_ID, List.of(100L))).willReturn(List.of());
        given(feedCommentQueryRepository.countActiveByVideoIds(List.of(100L))).willReturn(List.of());
        // 리졸버가 내 멤버십을 돌려주더라도 응답에는 실리지 않아야 한다.
        given(memberIdResolver.findMemberIds(eq(GROUP_ID), any())).willReturn(Map.of(USER_ID, 99L));

        FeedResponse response = feedService.getFeed(USER_ID, GROUP_ID, SERVICE_DATE, 0, 2);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.userId()).isEqualTo(USER_ID);
            assertThat(item.memberId()).isNull();
            assertThat(item.isMe()).isTrue();
        });
    }

    @Test
    @DisplayName("영상을 남기고 그룹을 나간 작성자는 memberId가 null이지만 isMe는 false다")
    void memberIdIsNullForAuthorWhoLeftTheGroup() {
        givenSingleVideoFeed(profile(user(2L), "동구", null));
        given(memberIdResolver.findMemberIds(eq(GROUP_ID), any())).willReturn(Map.of());

        FeedResponse response = feedService.getFeed(USER_ID, GROUP_ID, SERVICE_DATE, 0, 2);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.memberId()).isNull();
            // memberId가 null이라고 "내 영상"인 것은 아니다. 두 값이 독립임을 고정한다 —
            // 클라이언트가 memberId == null 로 본인 여부를 유추하면 나간 사람의 카드까지 내 것으로 취급한다.
            assertThat(item.isMe()).isFalse();
        });
    }

    @Test
    @DisplayName("프로필이 없는 작성자에게도 호칭은 적용된다")
    void appliesAliasEvenWhenAuthorHasNoProfile() {
        givenSingleVideoFeedWithoutProfile();
        given(aliasReader.findAliases(eq(GROUP_ID), eq(USER_ID), any())).willReturn(Map.of(2L, "막내"));

        FeedResponse response = feedService.getFeed(USER_ID, GROUP_ID, SERVICE_DATE, 0, 2);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.nickname()).isNull();
            assertThat(item.displayName()).isEqualTo("막내");
        });
    }

    /** 영상 1건짜리 열린 피드를 준비한다. 반환값은 작성자. */
    private User givenSingleVideoFeed(UserProfile authorProfile) {
        User author = givenSingleVideoFeedWithoutProfile();
        given(userProfileRepository.findAllById(any())).willReturn(List.of(authorProfile));
        return author;
    }

    private User givenSingleVideoFeedWithoutProfile() {
        User author = user(2L);
        Video video = video(100L, author, question(30L, "오늘 가장 웃겼던 일은?"),
                LocalDateTime.of(2026, 7, 24, 12, 0));

        given(dailyAnswerRepository.findByGroupIdAndUserIdAndServiceDate(GROUP_ID, USER_ID, SERVICE_DATE))
                .willReturn(Optional.of(answer(AnswerUploadStatus.UPLOADED)));
        given(videoRepository.findByGroupIdAndDailyAnswerServiceDateAndStatus(
                eq(GROUP_ID), eq(SERVICE_DATE), eq(VideoStatus.ACTIVE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(video), PageRequest.of(0, 2), 1));
        given(userProfileRepository.findAllById(any())).willReturn(List.of());
        given(feedReactionQueryRepository.countByVideoIds(List.of(100L))).willReturn(List.of());
        given(feedReactionQueryRepository.findReactedVideoIds(USER_ID, List.of(100L))).willReturn(List.of());
        given(feedCommentQueryRepository.countActiveByVideoIds(List.of(100L))).willReturn(List.of());
        return author;
    }

    private FeedCountProjection count(Long videoId, Long totalCount) {
        return new FeedCountProjection() {
            @Override
            public Long getVideoId() {
                return videoId;
            }

            @Override
            public Long getTotalCount() {
                return totalCount;
            }
        };
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
