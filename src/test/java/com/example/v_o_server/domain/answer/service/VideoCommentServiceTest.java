package com.example.v_o_server.domain.answer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.service.GroupMemberAliasReader;
import com.example.v_o_server.domain.group.service.GroupMemberIdResolver;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.entity.UserProfile;
import com.example.v_o_server.domain.user.repository.UserProfileRepository;
import com.example.v_o_server.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

// 공용 헬퍼(commentBy 등)가 테스트별로 일부만 쓰는 스텁을 만들기 때문에 LENIENT로 완화
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VideoCommentServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long OTHER_USER_ID = 20L;
    private static final Long VIDEO_ID = 100L;
    private static final Long COMMENT_ID = 1000L;
    private static final Long GROUP_ID = 500L;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private VideoCommentRepository videoCommentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private FeedAccessPolicy feedAccessPolicy;

    @Mock
    private GroupMemberAliasReader aliasReader;

    @Mock
    private GroupMemberIdResolver memberIdResolver;

    @InjectMocks
    private VideoCommentService videoCommentService;

    private Video activeVideo() {
        // 호칭은 "이 영상이 속한 그룹" 기준이라 group이 반드시 필요하다 (엔티티에서도 NOT NULL).
        PrivateGroup group = mock(PrivateGroup.class);
        given(group.getId()).willReturn(GROUP_ID);
        Video video = mock(Video.class);
        given(video.getGroup()).willReturn(group);
        given(videoRepository.findByIdAndStatus(VIDEO_ID, VideoStatus.ACTIVE)).willReturn(Optional.of(video));
        given(aliasReader.findAliases(eq(GROUP_ID), anyLong(), any())).willReturn(Map.of());
        given(memberIdResolver.findMemberIds(eq(GROUP_ID), any())).willReturn(Map.of());
        return video;
    }

    private VideoComment commentBy(Long writerId) {
        User writer = mock(User.class);
        given(writer.getId()).willReturn(writerId);
        VideoComment comment = mock(VideoComment.class);
        given(comment.getUser()).willReturn(writer);
        // 실제 엔티티의 isWrittenBy와 동일하게 동작하도록 위임
        given(comment.isWrittenBy(anyLong()))
                .willAnswer(invocation -> writerId.equals(invocation.getArgument(0)));
        return comment;
    }

    /* -------------------- 목록 조회 -------------------- */

    @Test
    @DisplayName("댓글 목록 조회 성공 - PAGE_SIZE 이하일 때 hasNext=false")
    void getComments_success() {
        Video video = activeVideo();
        given(feedAccessPolicy.canAccess(USER_ID, video)).willReturn(true);

        VideoComment comment = commentBy(OTHER_USER_ID);
        given(comment.getId()).willReturn(COMMENT_ID);
        given(comment.getContent()).willReturn("잘 봤어요");
        given(comment.getIsDeleted()).willReturn(false);
        given(videoCommentRepository.findByVideo_IdAndIsDeletedFalseAndIdGreaterThanOrderByIdAsc(
                eq(VIDEO_ID), eq(0L), any(Pageable.class))).willReturn(List.of(comment));

        UserProfile profile = mock(UserProfile.class);
        User profileUser = mock(User.class);
        given(profileUser.getId()).willReturn(OTHER_USER_ID);
        given(profile.getUser()).willReturn(profileUser);
        given(profile.getNickname()).willReturn("친구");
        given(profile.getProfileImageUrl()).willReturn("https://img/profile.png");
        given(userProfileRepository.findAllById(List.of(OTHER_USER_ID))).willReturn(List.of(profile));

        CommentListResponse response = videoCommentService.getComments(USER_ID, VIDEO_ID, null);

        assertThat(response.hasNext()).isFalse();
        assertThat(response.comments()).hasSize(1);
        CommentResponse first = response.comments().get(0);
        assertThat(first.commentId()).isEqualTo(COMMENT_ID);
        assertThat(first.isMine()).isFalse();
        assertThat(first.writer().nickname()).isEqualTo("친구");
        // 호칭을 지정하지 않았으면 alias는 비고 표시 이름은 전역 닉네임 그대로.
        assertThat(first.writer().alias()).isNull();
        assertThat(first.writer().displayName()).isEqualTo("친구");
    }

    @Test
    @DisplayName("댓글 목록 조회 - 내가 지정한 호칭이 있으면 작성자 표시 이름이 호칭으로 나온다")
    void getComments_appliesViewerAlias() {
        Video video = activeVideo();
        given(feedAccessPolicy.canAccess(USER_ID, video)).willReturn(true);

        VideoComment comment = commentBy(OTHER_USER_ID);
        given(comment.getId()).willReturn(COMMENT_ID);
        given(comment.getContent()).willReturn("허허허");
        given(videoCommentRepository.findByVideo_IdAndIsDeletedFalseAndIdGreaterThanOrderByIdAsc(
                eq(VIDEO_ID), eq(0L), any(Pageable.class))).willReturn(List.of(comment));

        UserProfile profile = mock(UserProfile.class);
        User profileUser = mock(User.class);
        given(profileUser.getId()).willReturn(OTHER_USER_ID);
        given(profile.getUser()).willReturn(profileUser);
        given(profile.getNickname()).willReturn("홍길동");
        given(userProfileRepository.findAllById(List.of(OTHER_USER_ID))).willReturn(List.of(profile));
        // 호칭은 "이 영상이 속한 그룹" + "조회자" 기준으로만 조회되어야 한다.
        given(aliasReader.findAliases(GROUP_ID, USER_ID, List.of(OTHER_USER_ID)))
                .willReturn(Map.of(OTHER_USER_ID, "아빠"));

        CommentListResponse response = videoCommentService.getComments(USER_ID, VIDEO_ID, null);

        CommentResponse.Writer writer = response.comments().get(0).writer();
        assertThat(writer.displayName()).isEqualTo("아빠");
        // 이름 편집 화면 입력 기본값으로 쓰도록 호칭 원본도 함께 내려준다.
        assertThat(writer.alias()).isEqualTo("아빠");
        assertThat(writer.nickname()).isEqualTo("홍길동");
        // 호칭 API 경로에 넣을 groupId도 응답에 실린다.
        assertThat(response.comments().get(0).groupId()).isEqualTo(GROUP_ID);
        verify(aliasReader).findAliases(GROUP_ID, USER_ID, List.of(OTHER_USER_ID));
    }

    @Test
    @DisplayName("댓글 목록 조회 - 작성자의 memberId를 실어 호칭 편집 화면으로 갈 수 있게 한다")
    void getComments_carriesMemberId() {
        Video video = activeVideo();
        given(feedAccessPolicy.canAccess(USER_ID, video)).willReturn(true);

        VideoComment comment = commentBy(OTHER_USER_ID);
        given(comment.getId()).willReturn(COMMENT_ID);
        given(comment.getContent()).willReturn("허허허");
        given(videoCommentRepository.findByVideo_IdAndIsDeletedFalseAndIdGreaterThanOrderByIdAsc(
                eq(VIDEO_ID), eq(0L), any(Pageable.class))).willReturn(List.of(comment));
        given(userProfileRepository.findAllById(List.of(OTHER_USER_ID))).willReturn(List.of());
        given(memberIdResolver.findMemberIds(GROUP_ID, List.of(OTHER_USER_ID)))
                .willReturn(Map.of(OTHER_USER_ID, 33L));

        CommentListResponse response = videoCommentService.getComments(USER_ID, VIDEO_ID, null);

        assertThat(response.comments().get(0).writer().memberId()).isEqualTo(33L);
        verify(memberIdResolver).findMemberIds(GROUP_ID, List.of(OTHER_USER_ID));
    }

    @Test
    @DisplayName("댓글 목록 조회 - 내가 예전에 쓴 댓글에는 memberId를 내리지 않는다")
    void getComments_memberIdIsNullForMyOwnComment() {
        Video video = activeVideo();
        given(feedAccessPolicy.canAccess(USER_ID, video)).willReturn(true);

        VideoComment myComment = commentBy(USER_ID);
        given(myComment.getId()).willReturn(COMMENT_ID);
        given(myComment.getContent()).willReturn("오늘 정말 좋았어");
        given(videoCommentRepository.findByVideo_IdAndIsDeletedFalseAndIdGreaterThanOrderByIdAsc(
                eq(VIDEO_ID), eq(0L), any(Pageable.class))).willReturn(List.of(myComment));
        given(userProfileRepository.findAllById(List.of(USER_ID))).willReturn(List.of());
        // 리졸버가 내 멤버십을 돌려주더라도 응답에는 실리지 않아야 한다.
        given(memberIdResolver.findMemberIds(GROUP_ID, List.of(USER_ID))).willReturn(Map.of(USER_ID, 99L));

        CommentListResponse response = videoCommentService.getComments(USER_ID, VIDEO_ID, null);

        CommentResponse first = response.comments().get(0);
        assertThat(first.isMine()).isTrue();
        assertThat(first.writer().memberId()).isNull();
    }

    @Test
    @DisplayName("댓글 목록 조회 - 댓글 남기고 나간 작성자는 memberId가 null이다")
    void getComments_memberIdIsNullForFormerMember() {
        Video video = activeVideo();
        given(feedAccessPolicy.canAccess(USER_ID, video)).willReturn(true);

        VideoComment comment = commentBy(OTHER_USER_ID);
        given(comment.getId()).willReturn(COMMENT_ID);
        given(comment.getContent()).willReturn("허허허");
        given(videoCommentRepository.findByVideo_IdAndIsDeletedFalseAndIdGreaterThanOrderByIdAsc(
                eq(VIDEO_ID), eq(0L), any(Pageable.class))).willReturn(List.of(comment));
        given(userProfileRepository.findAllById(List.of(OTHER_USER_ID))).willReturn(List.of());
        given(memberIdResolver.findMemberIds(GROUP_ID, List.of(OTHER_USER_ID))).willReturn(Map.of());

        CommentListResponse response = videoCommentService.getComments(USER_ID, VIDEO_ID, null);

        assertThat(response.comments().get(0).writer().memberId()).isNull();
    }

    @Test
    @DisplayName("댓글 등록 - 본인 댓글이므로 표시 이름은 항상 내 닉네임이고 memberId는 없다")
    void createComment_displayNameIsAlwaysOwnNickname() {
        Video video = activeVideo();
        given(feedAccessPolicy.canAccess(USER_ID, video)).willReturn(true);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(mock(User.class)));

        VideoComment saved = commentBy(USER_ID);
        given(saved.getId()).willReturn(COMMENT_ID);
        given(saved.getContent()).willReturn("오늘 정말 좋았어");
        given(videoCommentRepository.save(any(VideoComment.class))).willReturn(saved);

        UserProfile profile = mock(UserProfile.class);
        given(profile.getNickname()).willReturn("나");
        given(profile.getProfileImageUrl()).willReturn("https://img/me.png");
        given(userProfileRepository.findById(USER_ID)).willReturn(Optional.of(profile));

        CommentResponse response = videoCommentService.createComment(USER_ID, VIDEO_ID, "오늘 정말 좋았어");

        assertThat(response.writer().nickname()).isEqualTo("나");
        assertThat(response.writer().displayName()).isEqualTo("나");
        // 자기 자신에게는 호칭을 지정할 수 없으므로 alias·memberId 모두 내리지 않는다.
        assertThat(response.writer().alias()).isNull();
        assertThat(response.writer().memberId()).isNull();
        assertThat(response.groupId()).isEqualTo(GROUP_ID);
        assertThat(response.isMine()).isTrue();
    }

    @Test
    @DisplayName("댓글 목록 조회 - 페이지 크기 초과분이 있으면 hasNext=true, 초과분 제외")
    void getComments_hasNext() {
        Video video = activeVideo();
        given(feedAccessPolicy.canAccess(USER_ID, video)).willReturn(true);

        List<VideoComment> fetched = new java.util.ArrayList<>();
        for (long i = 1; i <= 21; i++) {
            VideoComment comment = commentBy(OTHER_USER_ID);
            given(comment.getId()).willReturn(i);
            fetched.add(comment);
        }
        given(videoCommentRepository.findByVideo_IdAndIsDeletedFalseAndIdGreaterThanOrderByIdAsc(
                eq(VIDEO_ID), eq(0L), any(Pageable.class))).willReturn(fetched);
        given(userProfileRepository.findAllById(any())).willReturn(List.of());

        CommentListResponse response = videoCommentService.getComments(USER_ID, VIDEO_ID, null);

        assertThat(response.hasNext()).isTrue();
        assertThat(response.comments()).hasSize(20);
    }

    @Test
    @DisplayName("댓글 목록 조회 실패 - 없는 영상이면 VIDEO_NOT_FOUND")
    void getComments_videoNotFound() {
        given(videoRepository.findByIdAndStatus(VIDEO_ID, VideoStatus.ACTIVE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> videoCommentService.getComments(USER_ID, VIDEO_ID, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VIDEO_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 목록 조회 실패 - 오늘의 답변 미완료면 FEED_LOCKED")
    void getComments_feedLocked() {
        Video video = activeVideo();
        given(feedAccessPolicy.canAccess(USER_ID, video)).willReturn(false);

        assertThatThrownBy(() -> videoCommentService.getComments(USER_ID, VIDEO_ID, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FEED_LOCKED);
    }

    /* -------------------- 등록 -------------------- */

    @Test
    @DisplayName("댓글 등록 성공 - isMine=true로 응답")
    void createComment_success() {
        Video video = activeVideo();
        given(feedAccessPolicy.canAccess(USER_ID, video)).willReturn(true);

        User user = mock(User.class);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        VideoComment saved = commentBy(USER_ID);
        given(saved.getId()).willReturn(COMMENT_ID);
        given(saved.getContent()).willReturn("첫 댓글");
        given(videoCommentRepository.save(any(VideoComment.class))).willReturn(saved);

        UserProfile profile = mock(UserProfile.class);
        given(profile.getNickname()).willReturn("나");
        given(profile.getProfileImageUrl()).willReturn(null);
        given(userProfileRepository.findById(USER_ID)).willReturn(Optional.of(profile));

        CommentResponse response = videoCommentService.createComment(USER_ID, VIDEO_ID, "첫 댓글");

        assertThat(response.commentId()).isEqualTo(COMMENT_ID);
        assertThat(response.isMine()).isTrue();
        assertThat(response.writer().nickname()).isEqualTo("나");
    }

    @Test
    @DisplayName("댓글 등록 실패 - 공백이면 COMMENT_BLANK")
    void createComment_blank() {
        assertThatThrownBy(() -> videoCommentService.createComment(USER_ID, VIDEO_ID, "   "))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_BLANK);
        verify(videoCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("댓글 등록 실패 - 100자 초과면 COMMENT_TOO_LONG")
    void createComment_tooLong() {
        String tooLong = "가".repeat(101);

        assertThatThrownBy(() -> videoCommentService.createComment(USER_ID, VIDEO_ID, tooLong))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_TOO_LONG);
    }

    @Test
    @DisplayName("댓글 등록 실패 - 없는 영상이면 VIDEO_NOT_FOUND")
    void createComment_videoNotFound() {
        given(videoRepository.findByIdAndStatus(VIDEO_ID, VideoStatus.ACTIVE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> videoCommentService.createComment(USER_ID, VIDEO_ID, "댓글"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VIDEO_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 등록 실패 - 오늘의 답변 미완료면 FEED_LOCKED")
    void createComment_feedLocked() {
        Video video = activeVideo();
        given(feedAccessPolicy.canAccess(USER_ID, video)).willReturn(false);

        assertThatThrownBy(() -> videoCommentService.createComment(USER_ID, VIDEO_ID, "댓글"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FEED_LOCKED);
    }

    /* -------------------- 수정 -------------------- */

    @Test
    @DisplayName("댓글 수정 성공 - 본인 댓글 내용 변경")
    void updateComment_success() {
        VideoComment comment = commentBy(USER_ID);
        given(comment.getId()).willReturn(COMMENT_ID);
        given(comment.getIsDeleted()).willReturn(false);
        given(comment.getContent()).willReturn("수정된 댓글");
        given(videoCommentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

        CommentUpdateResponse response = videoCommentService.updateComment(USER_ID, COMMENT_ID, "수정된 댓글");

        assertThat(response.commentId()).isEqualTo(COMMENT_ID);
        assertThat(response.content()).isEqualTo("수정된 댓글");
        assertThat(response.isMine()).isTrue();
        verify(comment).updateContent(eq("수정된 댓글"), any());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 작성자가 아니면 COMMENT_NO_PERMISSION")
    void updateComment_noPermission() {
        VideoComment comment = commentBy(OTHER_USER_ID);
        given(comment.getIsDeleted()).willReturn(false);
        given(videoCommentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> videoCommentService.updateComment(USER_ID, COMMENT_ID, "수정"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_NO_PERMISSION);
    }

    @Test
    @DisplayName("댓글 수정 실패 - 없는 댓글이면 COMMENT_NOT_FOUND")
    void updateComment_notFound() {
        given(videoCommentRepository.findById(COMMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> videoCommentService.updateComment(USER_ID, COMMENT_ID, "수정"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 수정 실패 - 이미 삭제된 댓글이면 COMMENT_NOT_FOUND")
    void updateComment_alreadyDeleted() {
        VideoComment comment = commentBy(USER_ID);
        given(comment.getIsDeleted()).willReturn(true);
        given(videoCommentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> videoCommentService.updateComment(USER_ID, COMMENT_ID, "수정"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 수정 실패 - 100자 초과면 COMMENT_TOO_LONG")
    void updateComment_tooLong() {
        assertThatThrownBy(() -> videoCommentService.updateComment(USER_ID, COMMENT_ID, "가".repeat(101)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_TOO_LONG);
    }

    @Test
    @DisplayName("댓글 수정 실패 - 공백이면 COMMENT_BLANK")
    void updateComment_blank() {
        assertThatThrownBy(() -> videoCommentService.updateComment(USER_ID, COMMENT_ID, ""))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_BLANK);
    }

    /* -------------------- 삭제 -------------------- */

    @Test
    @DisplayName("댓글 삭제 성공 - soft delete 처리")
    void deleteComment_success() {
        VideoComment comment = commentBy(USER_ID);
        given(comment.getIsDeleted()).willReturn(false);
        given(videoCommentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

        videoCommentService.deleteComment(USER_ID, COMMENT_ID);

        verify(comment).delete(any());
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 작성자가 아니면 COMMENT_NO_PERMISSION")
    void deleteComment_noPermission() {
        VideoComment comment = commentBy(OTHER_USER_ID);
        given(comment.getIsDeleted()).willReturn(false);
        given(videoCommentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> videoCommentService.deleteComment(USER_ID, COMMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_NO_PERMISSION);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 없는 댓글이면 COMMENT_NOT_FOUND")
    void deleteComment_notFound() {
        given(videoCommentRepository.findById(COMMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> videoCommentService.deleteComment(USER_ID, COMMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }
}
