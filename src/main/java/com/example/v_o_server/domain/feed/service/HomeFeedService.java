package com.example.v_o_server.domain.feed.service;

import static com.example.v_o_server.common.time.KoreaDateTime.toOffsetDateTime;

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
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import com.example.v_o_server.domain.group.service.GroupMemberAliasReader;
import com.example.v_o_server.domain.group.service.GroupMemberIdResolver;
import com.example.v_o_server.domain.user.entity.UserProfile;
import com.example.v_o_server.domain.user.repository.UserProfileRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내가 ACTIVE로 속한 모든 그룹의 오늘 영상을 한 타임라인으로 합쳐 보여주는 홈 피드.
 *
 * <p>{@link FeedService}와 아이템 조립 로직이 겹치지만, {@code FeedItemResponse}/{@code FeedResponse}는
 * 다른 진행 중인 변경이 소유한 파일이라 이번에는 공용 조립기로 추출하지 않고 이 클래스 안에 자체 매핑을 둔다
 * (docs/plans/comment-alias-and-home-feed.md §B-6). 공용화는 별도 리팩터링 이슈로 남긴다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeFeedService {

    private final GroupMemberRepository groupMemberRepository;
    private final DailyAnswerRepository dailyAnswerRepository;
    private final VideoRepository videoRepository;
    private final UserProfileRepository userProfileRepository;
    private final FeedReactionQueryRepository feedReactionQueryRepository;
    private final FeedCommentQueryRepository feedCommentQueryRepository;
    private final GroupMemberAliasReader aliasReader;
    private final GroupMemberIdResolver memberIdResolver;

    public HomeFeedResponse getHomeFeed(Long userId, LocalDate serviceDate, int page, int size) {
        // 내가 ACTIVE 멤버 + 그룹도 ACTIVE인 멤버십만 대상으로 한다. 조회 그룹 집합이 여기서 정해지므로
        // 이후 어떤 쿼리에도 groupId 파라미터가 없어 남의 그룹 영상이 섞여 들어올 경로가 없다.
        List<GroupMember> memberships = groupMemberRepository.findActiveMembershipsWithGroup(userId);
        if (memberships.isEmpty()) {
            return HomeFeedResponse.empty(serviceDate, page, size);
        }

        // group_members에 (group_id, user_id) 유니크 제약이 없어 같은 그룹에 ACTIVE 행이 중복될 수 있다.
        // 중복이면 카드가 두 번 그려지므로 groupId 기준으로 먼저 접고, 살아남는 행은 id가 작은(가장 오래된) 쪽으로 고정한다.
        Map<Long, GroupMember> membershipByGroupId = memberships.stream()
                .collect(Collectors.toMap(
                        member -> member.getGroup().getId(),
                        Function.identity(),
                        (a, b) -> a.getId() <= b.getId() ? a : b
                ));
        List<Long> groupIds = List.copyOf(membershipByGroupId.keySet());

        // daily_answers에도 (group_id, user_id, service_date) 유니크 제약이 없다. 중복 행이 있어도
        // 병합 함수 없는 toMap은 예외로 API 전체를 500으로 만들므로, 전순서 규칙(업로드 우선 → id 작은 쪽)으로 하나만 남긴다.
        Map<Long, DailyAnswer> answerByGroupId = dailyAnswerRepository
                .findByUserIdAndServiceDateAndGroupIdIn(userId, serviceDate, groupIds).stream()
                .collect(Collectors.toMap(
                        answer -> answer.getGroup().getId(),
                        Function.identity(),
                        HomeFeedService::pickAnswer
                ));

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("uploadedAt"), Sort.Order.desc("id"))
        );
        Page<Video> videos = videoRepository.findByGroupIdInAndDailyAnswerServiceDateAndStatus(
                groupIds,
                serviceDate,
                VideoStatus.ACTIVE,
                pageable
        );

        List<Long> videoIds = videos.stream().map(Video::getId).toList();
        Set<Long> authorIds = videos.stream()
                .map(video -> video.getUser().getId())
                .collect(Collectors.toSet());
        Map<Long, UserProfile> profilesByUserId = userProfileRepository.findAllById(authorIds)
                .stream()
                .collect(Collectors.toMap(UserProfile::getId, Function.identity()));
        Map<Long, Long> reactionCounts = countByVideoId(
                videoIds.isEmpty() ? List.of() : feedReactionQueryRepository.countByVideoIds(videoIds)
        );
        Map<Long, Long> commentCounts = countByVideoId(
                videoIds.isEmpty() ? List.of() : feedCommentQueryRepository.countActiveByVideoIds(videoIds)
        );
        Set<Long> reactedVideoIds = videoIds.isEmpty()
                ? Set.of()
                : Set.copyOf(feedReactionQueryRepository.findReactedVideoIds(userId, videoIds));
        // 호칭/memberId는 그룹 스코프로 조회한다 — 같은 사람이 그룹 A·B에 모두 있고 A에서만 호칭을 지정했다면
        // B 아이템에는 호칭이 붙으면 안 된다.
        Map<Long, Map<Long, String>> aliasesByGroup = aliasReader.findAliasesByGroup(groupIds, userId, authorIds);
        Map<Long, Map<Long, Long>> memberIdsByGroup = memberIdResolver.findMemberIdsByGroup(groupIds, authorIds);

        Page<HomeFeedItemResponse> items = videos.map(video -> {
            Long groupId = video.getGroup().getId();
            GroupMember membership = membershipByGroupId.get(groupId);
            DailyAnswer viewerAnswer = answerByGroupId.get(groupId);
            boolean groupUnlocked = viewerAnswer != null && viewerAnswer.isUploaded();
            boolean isMe = userId.equals(video.getUser().getId());
            Long authorId = video.getUser().getId();

            FeedItemResponse feedItem = toFeedItem(
                    video,
                    profilesByUserId.get(authorId),
                    aliasesByGroup.getOrDefault(groupId, Map.of()).get(authorId),
                    // 내 영상에는 편집 진입용 memberId를 내리지 않는다 — 자기 자신에게는 호칭을 지정할 수 없다(G017).
                    isMe ? null : memberIdsByGroup.getOrDefault(groupId, Map.of()).get(authorId),
                    isMe,
                    reactionCounts.getOrDefault(video.getId(), 0L),
                    reactedVideoIds.contains(video.getId()),
                    commentCounts.getOrDefault(video.getId(), 0L)
            );
            return new HomeFeedItemResponse(groupId, membership.getGroup().getName(), groupUnlocked, feedItem);
        });

        List<HomeFeedGroupResponse> groups = membershipByGroupId.values().stream()
                .sorted(Comparator.comparing(GroupMember::getJoinedAt)
                        .thenComparing(member -> member.getGroup().getId()))
                .map(member -> toGroupResponse(member, answerByGroupId.get(member.getGroup().getId())))
                .toList();

        return HomeFeedResponse.of(serviceDate, groups, items);
    }

    /**
     * daily_answers 중복 행에 대한 전순서 병합 규칙.
     * ① 업로드 완료(UPLOADED)인 쪽을 우선한다. ② 우선순위가 같으면(둘 다 업로드 완료거나 둘 다 아니면)
     * id가 작은 쪽을 택한다 — 조회 순서에 의존하지 않는 결정적 결과를 위해서다.
     */
    private static DailyAnswer pickAnswer(DailyAnswer a, DailyAnswer b) {
        if (a.isUploaded() != b.isUploaded()) {
            return a.isUploaded() ? a : b;
        }
        return a.getId() <= b.getId() ? a : b;
    }

    private HomeFeedGroupResponse toGroupResponse(GroupMember membership, DailyAnswer viewerAnswer) {
        AnswerUploadStatus status = viewerAnswer == null
                ? AnswerUploadStatus.NOT_UPLOADED
                : viewerAnswer.getStatus();
        boolean unlocked = viewerAnswer != null && viewerAnswer.isUploaded();
        return new HomeFeedGroupResponse(
                membership.getGroup().getId(),
                membership.getGroup().getName(),
                membership.getGroup().getGroupImageUrl(),
                unlocked,
                status
        );
    }

    private Map<Long, Long> countByVideoId(List<FeedCountProjection> counts) {
        return counts.stream().collect(Collectors.toMap(
                FeedCountProjection::getVideoId,
                FeedCountProjection::getTotalCount
        ));
    }

    private FeedItemResponse toFeedItem(
            Video video,
            UserProfile profile,
            String alias,
            Long memberId,
            boolean isMe,
            long reactionCount,
            boolean reactedByMe,
            long commentCount
    ) {
        String nickname = profile == null ? null : profile.getNickname();
        return new FeedItemResponse(
                video.getId(),
                video.getUser().getId(),
                memberId,
                isMe,
                nickname,
                alias,
                GroupMemberAliasReader.resolveDisplayName(alias, nickname),
                profile == null ? null : profile.getProfileImageUrl(),
                video.getQuestion().getId(),
                video.getQuestion().getContent(),
                video.getVideoUrl(),
                video.getThumbnailUrl(),
                video.getDurationMs(),
                reactionCount,
                reactedByMe,
                commentCount,
                video.getCapturedAt(),
                toOffsetDateTime(video.getUploadedAt())
        );
    }
}
