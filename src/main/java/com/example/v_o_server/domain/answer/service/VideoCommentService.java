package com.example.v_o_server.domain.answer.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.answer.dto.response.CommentListResponse;
import com.example.v_o_server.domain.answer.dto.response.CommentResponse;
import com.example.v_o_server.domain.answer.dto.response.CommentUpdateResponse;
import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.answer.entity.VideoComment;
import com.example.v_o_server.domain.answer.entity.VideoStatus;
import com.example.v_o_server.domain.answer.repository.VideoCommentRepository;
import com.example.v_o_server.domain.answer.repository.VideoRepository;
import com.example.v_o_server.domain.group.service.GroupMemberAliasReader;
import com.example.v_o_server.domain.group.service.GroupMemberIdResolver;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.entity.UserProfile;
import com.example.v_o_server.domain.user.repository.UserProfileRepository;
import com.example.v_o_server.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoCommentService {

    private static final int PAGE_SIZE = 20;
    private static final int MAX_CONTENT_LENGTH = 100;

    private final VideoRepository videoRepository;
    private final VideoCommentRepository videoCommentRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final FeedAccessPolicy feedAccessPolicy;
    private final GroupMemberAliasReader aliasReader;
    private final GroupMemberIdResolver memberIdResolver;

    public CommentListResponse getComments(Long userId, Long videoId, Long cursor) {
        Video video = getActiveVideo(videoId);
        checkFeedAccess(userId, video);

        Pageable limit = PageRequest.of(0, PAGE_SIZE + 1);
        long cursorId = cursor == null ? 0L : cursor;
        List<VideoComment> fetched =
                videoCommentRepository.findByVideo_IdAndIsDeletedFalseAndIdGreaterThanOrderByIdAsc(
                        videoId, cursorId, limit);

        boolean hasNext = fetched.size() > PAGE_SIZE;
        List<VideoComment> comments = hasNext ? fetched.subList(0, PAGE_SIZE) : fetched;

        Long groupId = video.getGroup().getId();
        List<Long> writerIds = writerIdsOf(comments);
        Map<Long, UserProfile> profiles = loadWriterProfiles(writerIds);
        // 호칭은 보는 사람 기준이라 조회자(userId)와 이 영상이 속한 그룹으로 한정해 한 번에 읽는다.
        // 프로필이 없는 작성자에게도 호칭은 붙을 수 있으므로 프로필 맵이 아니라 작성자 전체 집합으로 조회한다.
        Map<Long, String> aliases = aliasReader.findAliases(groupId, userId, writerIds);
        // 댓글에서 바로 호칭 편집 화면으로 갈 수 있도록 멤버 ID를 함께 내려준다.
        Map<Long, Long> memberIds = memberIdResolver.findMemberIds(groupId, writerIds);

        List<CommentResponse> responses = comments.stream()
                .map(comment -> CommentResponse.of(
                        comment, toWriter(comment, userId, profiles, aliases, memberIds), userId))
                .toList();

        return new CommentListResponse(responses, hasNext);
    }

    @Transactional
    public CommentResponse createComment(Long userId, Long videoId, String content) {
        String validated = validateContent(content);

        Video video = getActiveVideo(videoId);
        checkFeedAccess(userId, video);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        VideoComment comment = videoCommentRepository.save(VideoComment.builder()
                .video(video)
                .user(user)
                .content(validated)
                .isDeleted(false)
                .build());

        // 본인이 방금 쓴 댓글이다. 자기 자신에게는 호칭을 지정할 수 없으므로 표시 이름은 항상 닉네임이다.
        CommentResponse.Writer writer = userProfileRepository.findById(userId)
                .map(profile -> CommentResponse.Writer.withoutAlias(
                        userId, profile.getNickname(), profile.getProfileImageUrl()))
                .orElse(CommentResponse.Writer.withoutAlias(userId, null, null));

        return CommentResponse.of(comment, writer, userId);
    }

    @Transactional
    public CommentUpdateResponse updateComment(Long userId, Long commentId, String content) {
        String validated = validateContent(content);

        VideoComment comment = getComment(commentId);
        checkPermission(comment, userId);

        comment.updateContent(validated, LocalDateTime.now());
        return CommentUpdateResponse.of(comment, userId);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        VideoComment comment = getComment(commentId);
        checkPermission(comment, userId);

        comment.delete(LocalDateTime.now());
    }

    private Video getActiveVideo(Long videoId) {
        return videoRepository.findByIdAndStatus(videoId, VideoStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
    }

    private VideoComment getComment(Long commentId) {
        return videoCommentRepository.findById(commentId)
                .filter(comment -> !Boolean.TRUE.equals(comment.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void checkFeedAccess(Long userId, Video video) {
        if (!feedAccessPolicy.canAccess(userId, video)) {
            throw new BusinessException(ErrorCode.FEED_LOCKED);
        }
    }

    private void checkPermission(VideoComment comment, Long userId) {
        if (!comment.isWrittenBy(userId)) {
            throw new BusinessException(ErrorCode.COMMENT_NO_PERMISSION);
        }
    }

    /** 공백/길이 검증. DTO @Valid 대신 여기서 검증해 전용 에러 코드(V005/V006)로 응답한다. */
    private String validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.COMMENT_BLANK);
        }
        String trimmed = content.strip();
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.COMMENT_TOO_LONG);
        }
        return trimmed;
    }

    private List<Long> writerIdsOf(List<VideoComment> comments) {
        return comments.stream()
                .map(comment -> comment.getUser().getId())
                .distinct()
                .toList();
    }

    private Map<Long, UserProfile> loadWriterProfiles(List<Long> writerIds) {
        return userProfileRepository.findAllById(writerIds).stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));
    }

    private CommentResponse.Writer toWriter(VideoComment comment, Long viewerUserId,
                                            Map<Long, UserProfile> profiles,
                                            Map<Long, String> aliases, Map<Long, Long> memberIds) {
        Long writerId = comment.getUser().getId();
        UserProfile profile = profiles.get(writerId);
        String nickname = profile == null ? null : profile.getNickname();
        String profileImageUrl = profile == null ? null : profile.getProfileImageUrl();
        String alias = aliases.get(writerId);
        // 자기 자신에게는 호칭을 지정할 수 없으므로(G017) 내 댓글에는 편집 진입용 memberId를 내리지 않는다.
        Long memberId = writerId.equals(viewerUserId) ? null : memberIds.get(writerId);
        return new CommentResponse.Writer(
                writerId,
                memberId,
                nickname,
                GroupMemberAliasReader.resolveDisplayName(alias, nickname),
                profileImageUrl);
    }
}
