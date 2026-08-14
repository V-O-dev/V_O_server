package com.example.v_o_server.domain.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.v_o_server.domain.answer.entity.AnswerUploadStatus;
import com.example.v_o_server.domain.answer.entity.DailyAnswer;
import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.answer.entity.VideoStatus;
import com.example.v_o_server.domain.answer.repository.DailyAnswerRepository;
import com.example.v_o_server.domain.answer.repository.VideoRepository;
import com.example.v_o_server.domain.feed.dto.FeedItemResponse;
import com.example.v_o_server.domain.feed.dto.HomeFeedGroupResponse;
import com.example.v_o_server.domain.feed.dto.HomeFeedItemResponse;
import com.example.v_o_server.domain.feed.dto.HomeFeedResponse;
import com.example.v_o_server.domain.feed.repository.FeedCommentQueryRepository;
import com.example.v_o_server.domain.feed.repository.FeedCountProjection;
import com.example.v_o_server.domain.feed.repository.FeedReactionQueryRepository;
import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.GroupMemberRole;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("HomeFeedService")
class HomeFeedServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 8, 12);

    private GroupMemberRepository groupMemberRepository;
    private DailyAnswerRepository dailyAnswerRepository;
    private VideoRepository videoRepository;
    private UserProfileRepository userProfileRepository;
    private FeedReactionQueryRepository feedReactionQueryRepository;
    private FeedCommentQueryRepository feedCommentQueryRepository;
    private GroupMemberAliasReader aliasReader;
    private GroupMemberIdResolver memberIdResolver;
    private HomeFeedService homeFeedService;

    @BeforeEach
    void setUp() {
        groupMemberRepository = mock(GroupMemberRepository.class);
        dailyAnswerRepository = mock(DailyAnswerRepository.class);
        videoRepository = mock(VideoRepository.class);
        userProfileRepository = mock(UserProfileRepository.class);
        feedReactionQueryRepository = mock(FeedReactionQueryRepository.class);
        feedCommentQueryRepository = mock(FeedCommentQueryRepository.class);
        aliasReader = mock(GroupMemberAliasReader.class);
        given(aliasReader.findAliasesByGroup(any(), any(), any())).willReturn(Map.of());
        memberIdResolver = mock(GroupMemberIdResolver.class);
        given(memberIdResolver.findMemberIdsByGroup(any(), any())).willReturn(Map.of());
        given(userProfileRepository.findAllById(anyCollection())).willReturn(List.of());

        homeFeedService = new HomeFeedService(
                groupMemberRepository,
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
    @DisplayName("그룹 2개 중 1개만 답변해도 양쪽 그룹의 영상이 모두 items에 포함되고, 그룹별 unlocked가 각각 반영된다")
    void includesItemsFromBothLockedAndUnlockedGroups() {
        PrivateGroup groupA = group(10L, "가족", "https://cdn.example.com/a.jpg");
        PrivateGroup groupB = group(11L, "동아리", null);
        givenMemberships(
                membership(1L, groupA, USER_ID, LocalDateTime.of(2026, 1, 1, 0, 0)),
                membership(2L, groupB, USER_ID, LocalDateTime.of(2026, 2, 1, 0, 0))
        );
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of(answer(1L, groupA, AnswerUploadStatus.UPLOADED)));

        Video videoA = video(100L, groupA, author(2L), LocalDateTime.of(2026, 8, 12, 9, 0));
        Video videoB = video(101L, groupB, author(3L), LocalDateTime.of(2026, 8, 12, 10, 0));
        givenVideoPage(List.of(videoB, videoA), 2);

        HomeFeedResponse response = homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        assertThat(response.groups()).hasSize(2);
        assertThat(groupResponse(response, 10L).unlocked()).isTrue();
        assertThat(groupResponse(response, 10L).viewerAnswerStatus()).isEqualTo(AnswerUploadStatus.UPLOADED);
        assertThat(groupResponse(response, 11L).unlocked()).isFalse();
        assertThat(groupResponse(response, 11L).viewerAnswerStatus()).isEqualTo(AnswerUploadStatus.NOT_UPLOADED);

        assertThat(response.items()).hasSize(2);
        assertThat(itemResponse(response, 100L).unlocked()).isTrue();
        assertThat(itemResponse(response, 101L).unlocked()).isFalse();
    }

    @Test
    @DisplayName("영상이 0건인 그룹도 groups[]에는 포함된다 (상단 그룹 탭·답변 배너용)")
    void includesGroupsWithNoVideosToday() {
        PrivateGroup groupA = group(10L, "가족", null);
        PrivateGroup groupB = group(11L, "동아리", null);
        givenMemberships(
                membership(1L, groupA, USER_ID, LocalDateTime.of(2026, 1, 1, 0, 0)),
                membership(2L, groupB, USER_ID, LocalDateTime.of(2026, 2, 1, 0, 0))
        );
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of());
        givenVideoPage(List.of(), 0);

        HomeFeedResponse response = homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        assertThat(response.groups()).hasSize(2);
        assertThat(response.items()).isEmpty();
    }

    @Test
    @DisplayName("같은 작성자가 그룹 A·B에 모두 있을 때, A에서만 지정한 호칭은 A 아이템에만 적용된다 (그룹 스코프 격리)")
    void aliasIsIsolatedPerGroup() {
        PrivateGroup groupA = group(10L, "가족", null);
        PrivateGroup groupB = group(11L, "동아리", null);
        givenMemberships(
                membership(1L, groupA, USER_ID, LocalDateTime.of(2026, 1, 1, 0, 0)),
                membership(2L, groupB, USER_ID, LocalDateTime.of(2026, 2, 1, 0, 0))
        );
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of());

        User author = author(2L);
        Video videoA = video(100L, groupA, author, LocalDateTime.of(2026, 8, 12, 9, 0));
        Video videoB = video(101L, groupB, author, LocalDateTime.of(2026, 8, 12, 10, 0));
        givenVideoPage(List.of(videoB, videoA), 2);
        given(aliasReader.findAliasesByGroup(any(), eq(USER_ID), any()))
                .willReturn(Map.of(10L, Map.of(2L, "동생")));

        HomeFeedResponse response = homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        assertThat(itemResponse(response, 100L).video().alias()).isEqualTo("동생");
        assertThat(itemResponse(response, 101L).video().alias()).isNull();
    }

    @Test
    @DisplayName("본인이 올린 영상은 memberId가 null이다 — 자기 자신에게는 호칭을 지정할 수 없다")
    void memberIdIsNullForMyOwnVideo() {
        PrivateGroup groupA = group(10L, "가족", null);
        givenMemberships(membership(1L, groupA, USER_ID, LocalDateTime.of(2026, 1, 1, 0, 0)));
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of(answer(1L, groupA, AnswerUploadStatus.UPLOADED)));

        Video myVideo = video(100L, groupA, author(USER_ID), LocalDateTime.of(2026, 8, 12, 9, 0));
        givenVideoPage(List.of(myVideo), 1);
        // 리졸버가 내 멤버십을 돌려주더라도 응답에는 실리지 않아야 한다.
        given(memberIdResolver.findMemberIdsByGroup(any(), any())).willReturn(Map.of(10L, Map.of(USER_ID, 99L)));

        HomeFeedResponse response = homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        HomeFeedItemResponse item = itemResponse(response, 100L);
        assertThat(item.video().isMe()).isTrue();
        assertThat(item.video().memberId()).isNull();
    }

    @Test
    @DisplayName("ACTIVE 그룹이 하나도 없으면 groups·items 모두 빈 배열이고, 영상 조회 쿼리를 아예 호출하지 않는다")
    void returnsEmptyResponseWhenNoActiveGroups() {
        given(groupMemberRepository.findActiveMembershipsWithGroup(USER_ID)).willReturn(List.of());

        HomeFeedResponse response = homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        assertThat(response.groups()).isEmpty();
        assertThat(response.items()).isEmpty();
        assertThat(response.totalElements()).isZero();
        verify(videoRepository, never())
                .findByGroupIdInAndDailyAnswerServiceDateAndStatus(any(), any(), any(), any());
        verify(dailyAnswerRepository, never())
                .findByUserIdAndServiceDateAndGroupIdIn(any(), any(), any());
    }

    @Test
    @DisplayName("그룹이 섞여도 아이템은 uploadedAt 최신순으로 나온다 (리포지토리 정렬 파라미터 고정)")
    void sortsItemsByUploadedAtDescendingAcrossGroups() {
        PrivateGroup groupA = group(10L, "가족", null);
        PrivateGroup groupB = group(11L, "동아리", null);
        givenMemberships(
                membership(1L, groupA, USER_ID, LocalDateTime.of(2026, 1, 1, 0, 0)),
                membership(2L, groupB, USER_ID, LocalDateTime.of(2026, 2, 1, 0, 0))
        );
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of());
        givenVideoPage(List.of(), 0);

        homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(videoRepository).findByGroupIdInAndDailyAnswerServiceDateAndStatus(
                any(), eq(SERVICE_DATE), eq(VideoStatus.ACTIVE), pageableCaptor.capture());
        // 두 필드가 각각 내림차순인지만 보면 "id DESC, uploadedAt DESC"(주/보조 키가 뒤바뀐 것)도 통과한다.
        // uploadedAt이 1순위 정렬 키여야 하므로 Sort.Order 목록의 순서 자체를 확인한다.
        List<org.springframework.data.domain.Sort.Order> orders =
                pageableCaptor.getValue().getSort().stream().toList();
        assertThat(orders).extracting(org.springframework.data.domain.Sort.Order::getProperty)
                .containsExactly("uploadedAt", "id");
        assertThat(orders).allMatch(org.springframework.data.domain.Sort.Order::isDescending);
    }

    @Test
    @DisplayName("잠긴 그룹의 아이템도 해제된 그룹과 동일한 필드 구성으로 내려간다 (마스킹하지 않는다)")
    void lockedGroupItemHasSameShapeAsUnlockedItem() {
        PrivateGroup groupB = group(11L, "동아리", null);
        givenMemberships(membership(2L, groupB, USER_ID, LocalDateTime.of(2026, 2, 1, 0, 0)));
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of());

        Video videoB = video(101L, groupB, author(3L), LocalDateTime.of(2026, 8, 12, 10, 0));
        givenVideoPage(List.of(videoB), 1);

        HomeFeedResponse response = homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        HomeFeedItemResponse item = itemResponse(response, 101L);
        assertThat(item.unlocked()).isFalse();
        assertThat(item.video().videoUrl()).isEqualTo("https://cdn.example.com/video.mp4");
        assertThat(item.video().questionContent()).isEqualTo("오늘 질문");
    }

    @Test
    @DisplayName("LEFT·KICKED 멤버십이거나 그룹 자체가 INACTIVE면 조회 대상 groupId에서 빠진다")
    void excludesInactiveMembershipsFromQueryScope() {
        // findActiveMembershipsWithGroup 자체가 ACTIVE 멤버십 + ACTIVE 그룹만 돌려주므로,
        // 여기서 스텁으로 준 그룹들만 videoRepository 호출의 groupIds 인자로 넘어가야 한다.
        PrivateGroup groupA = group(10L, "가족", null);
        givenMemberships(membership(1L, groupA, USER_ID, LocalDateTime.of(2026, 1, 1, 0, 0)));
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of());
        givenVideoPage(List.of(), 0);

        homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> groupIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(videoRepository).findByGroupIdInAndDailyAnswerServiceDateAndStatus(
                groupIdsCaptor.capture(), eq(SERVICE_DATE), eq(VideoStatus.ACTIVE), any());
        assertThat(groupIdsCaptor.getValue()).containsExactly(10L);
    }

    @Test
    @DisplayName("같은 (그룹, 유저, 날짜)에 daily_answers 행이 2건이어도 500이 아니라 정상 응답한다 (병합 함수 회귀 방지)")
    void handlesDuplicateDailyAnswerRowsWithoutThrowing() {
        PrivateGroup groupA = group(10L, "가족", null);
        givenMemberships(membership(1L, groupA, USER_ID, LocalDateTime.of(2026, 1, 1, 0, 0)));
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of(
                        answer(5L, groupA, AnswerUploadStatus.NOT_UPLOADED),
                        answer(6L, groupA, AnswerUploadStatus.UPLOADED)
                ));
        givenVideoPage(List.of(), 0);

        HomeFeedResponse response = homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        assertThat(groupResponse(response, 10L).unlocked()).isTrue();
    }

    @Test
    @DisplayName("daily_answers 중복 병합은 리포지토리 반환 순서와 무관하다 (UPLOADED가 먼저 와도 그 쪽이 이긴다)")
    void mergesDuplicateAnswersRegardlessOfEncounterOrder() {
        // 위 handlesDuplicateDailyAnswerRowsWithoutThrowing과 정반대 순서(UPLOADED를 먼저)로 준다.
        // 두 테스트 모두 통과해야만 "UPLOADED가 이긴다"는 것이지 "나중 값이 이긴다"가 아님이 증명된다 —
        // 순서를 고정한 채로만 검증하면 (a, b) -> b 같은 틀린 병합 함수도 우연히 통과한다.
        PrivateGroup groupA = group(10L, "가족", null);
        givenMemberships(membership(1L, groupA, USER_ID, LocalDateTime.of(2026, 1, 1, 0, 0)));
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of(
                        answer(6L, groupA, AnswerUploadStatus.UPLOADED),
                        answer(5L, groupA, AnswerUploadStatus.NOT_UPLOADED)
                ));
        givenVideoPage(List.of(), 0);

        HomeFeedResponse response = homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        assertThat(groupResponse(response, 10L).unlocked()).isTrue();
    }

    @Test
    @DisplayName("중복 답변 중 UPLOADED가 아닌 두 행은 id가 작은 쪽의 상태가 채택된다 (전순서 규칙)")
    void picksSmallerIdWhenNeitherDuplicateAnswerIsUploaded() {
        PrivateGroup groupA = group(10L, "가족", null);
        givenMemberships(membership(1L, groupA, USER_ID, LocalDateTime.of(2026, 1, 1, 0, 0)));
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of(
                        answer(7L, groupA, AnswerUploadStatus.UPLOAD_FAILED),
                        answer(6L, groupA, AnswerUploadStatus.NOT_UPLOADED)
                ));
        givenVideoPage(List.of(), 0);

        HomeFeedResponse response = homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        assertThat(groupResponse(response, 10L).viewerAnswerStatus()).isEqualTo(AnswerUploadStatus.NOT_UPLOADED);
        assertThat(groupResponse(response, 10L).unlocked()).isFalse();
    }

    @Test
    @DisplayName("id가 작은 쪽이 먼저 와도(=리스트 앞쪽) 여전히 그 쪽이 채택된다 (순서 비의존 확인)")
    void picksSmallerIdRegardlessOfEncounterOrder() {
        // 바로 위 테스트와 입력 순서를 뒤집었다. id=6(작은 쪽)이 이번엔 리스트 맨 앞에 온다.
        // 이 테스트 없이는 "무조건 마지막 값 채택" 같은 틀린 병합 함수도 우연히 통과한다.
        PrivateGroup groupA = group(10L, "가족", null);
        givenMemberships(membership(1L, groupA, USER_ID, LocalDateTime.of(2026, 1, 1, 0, 0)));
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of(
                        answer(6L, groupA, AnswerUploadStatus.NOT_UPLOADED),
                        answer(7L, groupA, AnswerUploadStatus.UPLOAD_FAILED)
                ));
        givenVideoPage(List.of(), 0);

        HomeFeedResponse response = homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        assertThat(groupResponse(response, 10L).viewerAnswerStatus()).isEqualTo(AnswerUploadStatus.NOT_UPLOADED);
    }

    @Test
    @DisplayName("같은 그룹에 중복 ACTIVE 멤버십이 있어도 groups[]에는 한 번만 나오고, 채택되는 건 id가 작은 쪽이다")
    void deduplicatesDuplicateActiveMembershipByGroupId() {
        // "groups[]에 1번만 나온다"는 것만으로는 병합 함수가 실제로 id가 작은 쪽을 택했는지 알 수 없다 —
        // HomeFeedGroupResponse에는 memberId/id가 노출되지 않기 때문이다. 대신 joinedAt이 정렬 키라는 점을
        // 이용해, 세 번째 그룹을 두 중복 행의 joinedAt 사이에 끼워 넣어 정렬 순서로 승자를 관측 가능하게 만든다.
        // id=3(2026-01-01)이 이겨야 맞고, 잘못 구현되어 id=9(2026-05-01)가 이기면 groupB가 앞으로 온다.
        PrivateGroup groupA = group(10L, "가족", null);
        PrivateGroup groupB = group(20L, "동아리", null);
        givenMemberships(
                membership(9L, groupA, USER_ID, LocalDateTime.of(2026, 5, 1, 0, 0)),
                membership(3L, groupA, USER_ID, LocalDateTime.of(2026, 1, 1, 0, 0)),
                membership(4L, groupB, USER_ID, LocalDateTime.of(2026, 3, 1, 0, 0))
        );
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of());
        givenVideoPage(List.of(), 0);

        HomeFeedResponse response = homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        assertThat(response.groups()).hasSize(2);
        assertThat(response.groups()).extracting(HomeFeedGroupResponse::groupId)
                .containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("중복 멤버십의 승자는 리포지토리 반환 순서와 무관하다 (id가 작은 쪽이 먼저 와도 동일하게 이긴다)")
    void deduplicatesDuplicateActiveMembershipRegardlessOfEncounterOrder() {
        // 바로 위 테스트와 입력 순서만 뒤집었다(id=3을 먼저). 두 테스트가 모두 같은 결과를 내야만
        // 병합 함수가 "id가 작은 쪽"을 실제로 비교하는 것이지 "리스트의 특정 위치"를 고르는 게 아님이 증명된다.
        PrivateGroup groupA = group(10L, "가족", null);
        PrivateGroup groupB = group(20L, "동아리", null);
        givenMemberships(
                membership(3L, groupA, USER_ID, LocalDateTime.of(2026, 1, 1, 0, 0)),
                membership(9L, groupA, USER_ID, LocalDateTime.of(2026, 5, 1, 0, 0)),
                membership(4L, groupB, USER_ID, LocalDateTime.of(2026, 3, 1, 0, 0))
        );
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of());
        givenVideoPage(List.of(), 0);

        HomeFeedResponse response = homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        assertThat(response.groups()).hasSize(2);
        assertThat(response.groups()).extracting(HomeFeedGroupResponse::groupId)
                .containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("groups[]는 joinedAt 오름차순으로 정렬되고, joinedAt이 같으면 groupId 오름차순으로 안정 정렬된다")
    void sortsGroupsByJoinedAtThenGroupId() {
        PrivateGroup groupC = group(30L, "동호회", null);
        PrivateGroup groupA = group(10L, "가족", null);
        PrivateGroup groupB = group(20L, "연인", null);
        LocalDateTime sameInstant = LocalDateTime.of(2026, 1, 1, 0, 0);
        givenMemberships(
                membership(1L, groupC, USER_ID, LocalDateTime.of(2026, 3, 1, 0, 0)),
                membership(2L, groupB, USER_ID, sameInstant),
                membership(3L, groupA, USER_ID, sameInstant)
        );
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of());
        givenVideoPage(List.of(), 0);

        HomeFeedResponse response = homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        assertThat(response.groups()).extracting(HomeFeedGroupResponse::groupId)
                .containsExactly(10L, 20L, 30L);
    }

    @Test
    @DisplayName("작성자 프로필·좋아요/댓글 수가 videoId 기준으로 정확히 매핑된다 (0으로 뭉개지지 않는다)")
    void populatesProfileReactionAndCommentCountsForCorrectVideo() {
        PrivateGroup groupA = group(10L, "가족", null);
        givenMemberships(membership(1L, groupA, USER_ID, LocalDateTime.of(2026, 1, 1, 0, 0)));
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of(answer(1L, groupA, AnswerUploadStatus.UPLOADED)));

        User author = author(2L);
        Video target = video(100L, groupA, author, LocalDateTime.of(2026, 8, 12, 9, 0));
        Video other = video(200L, groupA, author, LocalDateTime.of(2026, 8, 12, 8, 0));
        givenVideoPage(List.of(target, other), 2);
        given(userProfileRepository.findAllById(anyCollection()))
                .willReturn(List.of(profile(author, "동구", "https://cdn.example.com/profile.jpg")));
        // 두 영상 모두 응답에 실리므로, videoId로 정확히 갈라지는지 서로 다른 카운트를 준다.
        given(feedReactionQueryRepository.countByVideoIds(any()))
                .willReturn(List.of(count(100L, 7L), count(200L, 1L)));
        given(feedReactionQueryRepository.findReactedVideoIds(eq(USER_ID), any())).willReturn(List.of(100L));
        given(feedCommentQueryRepository.countActiveByVideoIds(any()))
                .willReturn(List.of(count(100L, 3L), count(200L, 9L)));

        HomeFeedResponse response = homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        FeedItemResponse targetVideo = itemResponse(response, 100L).video();
        assertThat(targetVideo.nickname()).isEqualTo("동구");
        assertThat(targetVideo.profileImageUrl()).isEqualTo("https://cdn.example.com/profile.jpg");
        assertThat(targetVideo.reactionCount()).isEqualTo(7L);
        assertThat(targetVideo.reactedByMe()).isTrue();
        assertThat(targetVideo.commentCount()).isEqualTo(3L);

        FeedItemResponse otherVideo = itemResponse(response, 200L).video();
        assertThat(otherVideo.reactionCount()).isEqualTo(1L);
        assertThat(otherVideo.reactedByMe()).isFalse();
        assertThat(otherVideo.commentCount()).isEqualTo(9L);
    }

    @Test
    @DisplayName("같은 사용자가 그룹 A·B에 서로 다른 group_members.id로 있으면, 각 아이템에는 그 그룹의 memberId가 붙는다")
    void resolvesMemberIdPerGroupScope() {
        PrivateGroup groupA = group(10L, "가족", null);
        PrivateGroup groupB = group(11L, "동아리", null);
        givenMemberships(
                membership(1L, groupA, USER_ID, LocalDateTime.of(2026, 1, 1, 0, 0)),
                membership(2L, groupB, USER_ID, LocalDateTime.of(2026, 2, 1, 0, 0))
        );
        given(dailyAnswerRepository.findByUserIdAndServiceDateAndGroupIdIn(eq(USER_ID), eq(SERVICE_DATE), any()))
                .willReturn(List.of());

        User author = author(2L);
        Video videoA = video(100L, groupA, author, LocalDateTime.of(2026, 8, 12, 9, 0));
        Video videoB = video(101L, groupB, author, LocalDateTime.of(2026, 8, 12, 10, 0));
        givenVideoPage(List.of(videoB, videoA), 2);
        given(memberIdResolver.findMemberIdsByGroup(any(), any()))
                .willReturn(Map.of(10L, Map.of(2L, 21L), 11L, Map.of(2L, 33L)));

        HomeFeedResponse response = homeFeedService.getHomeFeed(USER_ID, SERVICE_DATE, 0, 20);

        assertThat(itemResponse(response, 100L).video().memberId()).isEqualTo(21L);
        assertThat(itemResponse(response, 101L).video().memberId()).isEqualTo(33L);
    }

    private void givenMemberships(GroupMember... memberships) {
        given(groupMemberRepository.findActiveMembershipsWithGroup(USER_ID)).willReturn(List.of(memberships));
    }

    private void givenVideoPage(List<Video> videos, long totalElements) {
        given(videoRepository.findByGroupIdInAndDailyAnswerServiceDateAndStatus(
                any(), eq(SERVICE_DATE), eq(VideoStatus.ACTIVE), any(Pageable.class)))
                .willReturn(new PageImpl<>(videos, PageRequest.of(0, 20), totalElements));
    }

    private static HomeFeedGroupResponse groupResponse(HomeFeedResponse response, Long groupId) {
        return response.groups().stream()
                .filter(g -> g.groupId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("group not found: " + groupId));
    }

    private static HomeFeedItemResponse itemResponse(HomeFeedResponse response, Long videoId) {
        return response.items().stream()
                .filter(i -> i.video().videoId().equals(videoId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("item not found: " + videoId));
    }

    private PrivateGroup group(Long id, String name, String imageUrl) {
        PrivateGroup group = PrivateGroup.builder()
                .name(name)
                .groupImageUrl(imageUrl)
                .build();
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    private GroupMember membership(Long id, PrivateGroup group, Long userId, LocalDateTime joinedAt) {
        GroupMember member = GroupMember.builder()
                .group(group)
                .user(author(userId))
                .role(GroupMemberRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .joinedAt(joinedAt)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private DailyAnswer answer(Long id, PrivateGroup group, AnswerUploadStatus status) {
        DailyAnswer answer = DailyAnswer.builder()
                .group(group)
                .serviceDate(SERVICE_DATE)
                .status(status)
                .build();
        ReflectionTestUtils.setField(answer, "id", id);
        return answer;
    }

    private User author(Long id) {
        User user = User.builder()
                .status(UserStatus.ACTIVE)
                .dailyQuestionNotificationEnabled(true)
                .interactionNotificationEnabled(true)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Video video(Long id, PrivateGroup group, User author, LocalDateTime uploadedAt) {
        Question question = Question.builder()
                .content("오늘 질문")
                .status(QuestionStatus.ACTIVE)
                .answerTimeLimitMs(10_000)
                .useCount(0)
                .build();
        ReflectionTestUtils.setField(question, "id", 50L);

        Video video = Video.builder()
                .group(group)
                .user(author)
                .question(question)
                .videoUrl("https://cdn.example.com/video.mp4")
                .thumbnailUrl(null)
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
}
