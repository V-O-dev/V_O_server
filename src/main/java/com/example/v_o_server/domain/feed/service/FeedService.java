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

        if (viewerAnswer == null || !viewerAnswer.isUploaded()) {
            return FeedResponse.locked(serviceDate, viewerStatus, page, size);
        }

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
        Map<Long, UserProfile> profilesByUserId = userProfileRepository.findAllById(
                        videos.stream().map(video -> video.getUser().getId()).collect(Collectors.toSet())
                )
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

        Page<FeedItemResponse> items = videos.map(video ->
                toFeedItem(
                        video,
                        profilesByUserId.get(video.getUser().getId()),
                        reactionCounts.getOrDefault(video.getId(), 0L),
                        reactedVideoIds.contains(video.getId()),
                        commentCounts.getOrDefault(video.getId(), 0L)
                ));
        return FeedResponse.unlocked(serviceDate, viewerStatus, items);
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
            long reactionCount,
            boolean reactedByMe,
            long commentCount
    ) {
        return new FeedItemResponse(
                video.getId(),
                video.getUser().getId(),
                profile == null ? null : profile.getNickname(),
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
