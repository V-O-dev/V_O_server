package com.example.v_o_server.domain.answer.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.answer.dto.response.ReactionResponse;
import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.answer.entity.VideoReaction;
import com.example.v_o_server.domain.answer.entity.VideoStatus;
import com.example.v_o_server.domain.answer.repository.VideoReactionRepository;
import com.example.v_o_server.domain.answer.repository.VideoRepository;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VideoReactionService {

    private final VideoRepository videoRepository;
    private final VideoReactionRepository videoReactionRepository;
    private final UserRepository userRepository;
    private final FeedAccessPolicy feedAccessPolicy;
    private final ReactionRateLimiter reactionRateLimiter;

    public ReactionResponse addReaction(Long userId, Long videoId) {
        reactionRateLimiter.checkRate(userId, videoId);

        Video video = getActiveVideo(videoId);
        checkFeedAccess(userId, video);

        // TODO: 이미 좋아요를 누른 상태에서 재요청 시 정책 미확정 — 일단 멱등 처리(무시하고 현재 상태 반환)
        if (!videoReactionRepository.existsByVideo_IdAndUser_Id(videoId, userId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            videoReactionRepository.save(VideoReaction.builder()
                    .video(video)
                    .user(user)
                    .build());
        }

        return new ReactionResponse(videoId, videoReactionRepository.countByVideo_Id(videoId), true);
    }

    public ReactionResponse cancelReaction(Long userId, Long videoId) {
        Video video = getActiveVideo(videoId);

        videoReactionRepository.findByVideo_IdAndUser_Id(video.getId(), userId)
                .ifPresent(videoReactionRepository::delete);

        return new ReactionResponse(videoId, videoReactionRepository.countByVideo_Id(videoId), false);
    }

    private Video getActiveVideo(Long videoId) {
        return videoRepository.findByIdAndStatus(videoId, VideoStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
    }

    private void checkFeedAccess(Long userId, Video video) {
        if (!feedAccessPolicy.canAccess(userId, video)) {
            throw new BusinessException(ErrorCode.FEED_LOCKED);
        }
    }
}
