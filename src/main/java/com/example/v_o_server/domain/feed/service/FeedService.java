package com.example.v_o_server.domain.feed.service;

import com.example.v_o_server.domain.answer.entity.AnswerUploadStatus;
import com.example.v_o_server.domain.answer.entity.DailyAnswer;
import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.answer.entity.VideoStatus;
import com.example.v_o_server.domain.answer.repository.DailyAnswerRepository;
import com.example.v_o_server.domain.answer.repository.VideoRepository;
import com.example.v_o_server.domain.feed.dto.FeedItemResponse;
import com.example.v_o_server.domain.feed.dto.FeedResponse;
import com.example.v_o_server.domain.feed.repository.FeedCommentQueryRepository;
import com.example.v_o_server.domain.feed.repository.FeedCountProjection;
import com.example.v_o_server.domain.feed.repository.FeedReactionQueryRepository;
import com.example.v_o_server.domain.group.service.GroupAccessGuard;
import com.example.v_o_server.domain.group.service.GroupMemberAliasReader;
import com.example.v_o_server.domain.group.service.GroupMemberIdResolver;
import com.example.v_o_server.domain.user.entity.UserProfile;
import com.example.v_o_server.domain.user.repository.UserProfileRepository;
import java.time.LocalDate;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final GroupAccessGuard groupAccessGuard;
    private final DailyAnswerRepository dailyAnswerRepository;
    private final VideoRepository videoRepository;
    private final UserProfileRepository userProfileRepository;
    private final FeedReactionQueryRepository feedReactionQueryRepository;
    private final FeedCommentQueryRepository feedCommentQueryRepository;
    private final GroupMemberAliasReader aliasReader;
    private final GroupMemberIdResolver memberIdResolver;

    public FeedResponse getFeed(
            Long userId,
            Long groupId,
            LocalDate serviceDate,
            int page,
            int size
    ) {
        groupAccessGuard.getActiveGroup(groupId);
        groupAccessGuard.assertMember(groupId, userId);

        DailyAnswer viewerAnswer = dailyAnswerRepository
                .findByGroupIdAndUserIdAndServiceDate(groupId, userId, serviceDate)
                .orElse(null);
        AnswerUploadStatus viewerStatus = viewerAnswer == null
                ? AnswerUploadStatus.NOT_UPLOADED
                : viewerAnswer.getStatus();
        boolean unlocked = viewerAnswer != null && viewerAnswer.isUploaded();

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("uploadedAt"), Sort.Order.desc("id"))
        );
        Page<Video> videos = videoRepository.findByGroupIdAndDailyAnswerServiceDateAndStatus(
                groupId,
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
        // 호칭은 보는 사람 기준이라 뷰어(userId)와 이 그룹으로 한정해 한 번에 읽는다.
        // 프로필이 없는 작성자에게도 호칭은 붙을 수 있으므로 프로필 맵이 아니라 작성자 전체 집합으로 조회한다.
        Map<Long, String> aliases = aliasReader.findAliases(groupId, userId, authorIds);
        // 피드 카드에서 바로 호칭 편집 화면으로 갈 수 있도록 멤버 ID를 함께 내려준다(FED_BLR_01).
        Map<Long, Long> memberIds = memberIdResolver.findMemberIds(groupId, authorIds);

        Page<FeedItemResponse> items = videos.map(video ->
                toFeedItem(
                        video,
                        profilesByUserId.get(video.getUser().getId()),
                        aliases.get(video.getUser().getId()),
                        // 내 영상에는 편집 진입용 memberId를 내리지 않는다 — 자기 자신에게는 호칭을 지정할 수 없다(G017).
                        userId.equals(video.getUser().getId())
                                ? null : memberIds.get(video.getUser().getId()),
                        reactionCounts.getOrDefault(video.getId(), 0L),
                        reactedVideoIds.contains(video.getId()),
                        commentCounts.getOrDefault(video.getId(), 0L)
                ));
        return unlocked
                ? FeedResponse.unlocked(serviceDate, viewerStatus, items)
                : FeedResponse.locked(serviceDate, viewerStatus, items);
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
            long reactionCount,
            boolean reactedByMe,
            long commentCount
    ) {
        String nickname = profile == null ? null : profile.getNickname();
        return new FeedItemResponse(
                video.getId(),
                video.getUser().getId(),
                memberId,
                nickname,
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
                video.getUploadedAt()
        );
    }
}
