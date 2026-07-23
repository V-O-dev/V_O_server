package com.example.v_o_server.domain.answer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VideoReactionServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long VIDEO_ID = 100L;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private VideoReactionRepository videoReactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FeedAccessPolicy feedAccessPolicy;

    @Mock
    private ReactionRateLimiter reactionRateLimiter;

    @InjectMocks
    private VideoReactionService videoReactionService;

    private Video video;
    private User user;

    @BeforeEach
    void setUp() {
        video = org.mockito.Mockito.mock(Video.class);
        user = org.mockito.Mockito.mock(User.class);
    }

    @Test
    @DisplayName("좋아요 추가 성공 - 처음 누르는 경우 저장되고 isLiked=true")
    void addReaction_success() {
        given(videoRepository.findByIdAndStatus(VIDEO_ID, VideoStatus.ACTIVE)).willReturn(Optional.of(video));
        given(feedAccessPolicy.canAccess(USER_ID, video)).willReturn(true);
        given(videoReactionRepository.existsByVideo_IdAndUser_Id(VIDEO_ID, USER_ID)).willReturn(false);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(videoReactionRepository.countByVideo_Id(VIDEO_ID)).willReturn(1L);

        ReactionResponse response = videoReactionService.addReaction(USER_ID, VIDEO_ID);

        assertThat(response.videoId()).isEqualTo(VIDEO_ID);
        assertThat(response.likeCount()).isEqualTo(1L);
        assertThat(response.isLiked()).isTrue();
        verify(videoReactionRepository).save(any(VideoReaction.class));
    }

    @Test
    @DisplayName("좋아요 추가 - 이미 누른 상태면 멱등 처리 (저장 없음, isLiked=true)")
    void addReaction_alreadyLiked_idempotent() {
        given(videoRepository.findByIdAndStatus(VIDEO_ID, VideoStatus.ACTIVE)).willReturn(Optional.of(video));
        given(feedAccessPolicy.canAccess(USER_ID, video)).willReturn(true);
        given(videoReactionRepository.existsByVideo_IdAndUser_Id(VIDEO_ID, USER_ID)).willReturn(true);
        given(videoReactionRepository.countByVideo_Id(VIDEO_ID)).willReturn(3L);

        ReactionResponse response = videoReactionService.addReaction(USER_ID, VIDEO_ID);

        assertThat(response.likeCount()).isEqualTo(3L);
        assertThat(response.isLiked()).isTrue();
        verify(videoReactionRepository, never()).save(any(VideoReaction.class));
    }

    @Test
    @DisplayName("좋아요 추가 실패 - 없는 영상이면 VIDEO_NOT_FOUND")
    void addReaction_videoNotFound() {
        given(videoRepository.findByIdAndStatus(VIDEO_ID, VideoStatus.ACTIVE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> videoReactionService.addReaction(USER_ID, VIDEO_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VIDEO_NOT_FOUND);
    }

    @Test
    @DisplayName("좋아요 추가 실패 - 오늘의 답변 미완료면 FEED_LOCKED")
    void addReaction_feedLocked() {
        given(videoRepository.findByIdAndStatus(VIDEO_ID, VideoStatus.ACTIVE)).willReturn(Optional.of(video));
        given(feedAccessPolicy.canAccess(USER_ID, video)).willReturn(false);

        assertThatThrownBy(() -> videoReactionService.addReaction(USER_ID, VIDEO_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FEED_LOCKED);
    }

    @Test
    @DisplayName("좋아요 추가 실패 - 연타 시 TOO_MANY_REQUESTS")
    void addReaction_rateLimited() {
        willThrow(new BusinessException(ErrorCode.TOO_MANY_REQUESTS))
                .given(reactionRateLimiter).checkRate(USER_ID, VIDEO_ID);

        assertThatThrownBy(() -> videoReactionService.addReaction(USER_ID, VIDEO_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
        verify(videoRepository, never()).findByIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("좋아요 취소 성공 - 기존 리액션 삭제 후 isLiked=false")
    void cancelReaction_success() {
        VideoReaction reaction = org.mockito.Mockito.mock(VideoReaction.class);
        given(video.getId()).willReturn(VIDEO_ID);
        given(videoRepository.findByIdAndStatus(VIDEO_ID, VideoStatus.ACTIVE)).willReturn(Optional.of(video));
        given(videoReactionRepository.findByVideo_IdAndUser_Id(VIDEO_ID, USER_ID)).willReturn(Optional.of(reaction));
        given(videoReactionRepository.countByVideo_Id(VIDEO_ID)).willReturn(0L);

        ReactionResponse response = videoReactionService.cancelReaction(USER_ID, VIDEO_ID);

        assertThat(response.isLiked()).isFalse();
        assertThat(response.likeCount()).isZero();
        verify(videoReactionRepository).delete(reaction);
    }

    @Test
    @DisplayName("좋아요 취소 실패 - 없는 영상이면 VIDEO_NOT_FOUND")
    void cancelReaction_videoNotFound() {
        given(videoRepository.findByIdAndStatus(VIDEO_ID, VideoStatus.ACTIVE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> videoReactionService.cancelReaction(USER_ID, VIDEO_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VIDEO_NOT_FOUND);
    }

    @Test
    @DisplayName("좋아요 취소 - 누른 적 없어도 에러 없이 isLiked=false 반환 (멱등)")
    void cancelReaction_notLiked_idempotent() {
        given(video.getId()).willReturn(VIDEO_ID);
        given(videoRepository.findByIdAndStatus(VIDEO_ID, VideoStatus.ACTIVE)).willReturn(Optional.of(video));
        given(videoReactionRepository.findByVideo_IdAndUser_Id(VIDEO_ID, USER_ID)).willReturn(Optional.empty());
        given(videoReactionRepository.countByVideo_Id(VIDEO_ID)).willReturn(2L);

        ReactionResponse response = videoReactionService.cancelReaction(USER_ID, VIDEO_ID);

        assertThat(response.isLiked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(2L);
        verify(videoReactionRepository, never()).delete(any(VideoReaction.class));
    }
}
