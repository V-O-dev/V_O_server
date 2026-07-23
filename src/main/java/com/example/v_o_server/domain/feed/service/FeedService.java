package com.example.v_o_server.domain.feed.service;

import com.example.v_o_server.domain.answer.entity.AnswerUploadStatus;
import com.example.v_o_server.domain.answer.entity.DailyAnswer;
import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.answer.entity.VideoStatus;
import com.example.v_o_server.domain.answer.repository.DailyAnswerRepository;
import com.example.v_o_server.domain.answer.repository.VideoRepository;
import com.example.v_o_server.domain.feed.dto.FeedItemResponse;
import com.example.v_o_server.domain.feed.dto.FeedResponse;
import com.example.v_o_server.domain.group.service.GroupAccessGuard;
import com.example.v_o_server.domain.user.entity.UserProfile;
import com.example.v_o_server.domain.user.repository.UserProfileRepository;
import java.time.LocalDate;
import java.util.Map;
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

        Map<Long, UserProfile> profilesByUserId = userProfileRepository.findAllById(
                        videos.stream().map(video -> video.getUser().getId()).collect(Collectors.toSet())
                )
                .stream()
                .collect(Collectors.toMap(UserProfile::getId, Function.identity()));

        Page<FeedItemResponse> items = videos.map(video ->
                toFeedItem(video, profilesByUserId.get(video.getUser().getId())));
        return FeedResponse.unlocked(serviceDate, viewerStatus, items);
    }

    private FeedItemResponse toFeedItem(Video video, UserProfile profile) {
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
                video.getCapturedAt(),
                video.getUploadedAt()
        );
    }
}
