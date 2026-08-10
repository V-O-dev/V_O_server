package com.example.v_o_server.domain.answer.dto.response;

import com.example.v_o_server.domain.answer.entity.VideoComment;
import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
        /**
         * 이 댓글이 달린 영상의 그룹 ID.
         *
         * <p>호칭 API 경로가 {@code /groups/{groupId}/members/{memberId}/alias}라, 댓글에서 이름 편집 화면으로
         * 가려면 클라이언트가 그룹을 알아야 한다. 그룹 피드를 거쳐 들어온 경우엔 화면 상태로 알 수 있지만
         * 딥링크·알림에서 영상으로 바로 진입하면 알 수 없어서 응답에 함께 싣는다.</p>
         */
        Long groupId,
        String content,
        Writer writer,
        boolean isMine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record Writer(
            Long userId,
            /**
             * 작성자의 그룹 멤버 ID (group_members.id). 호칭 편집 화면으로 이동할 때 쓴다.
             * 내 댓글이거나 댓글을 남긴 뒤 그룹을 나갔으면 null이며, 이 경우 호칭을 지정할 수 없다.
             */
            Long memberId,
            String nickname,
            /**
             * 조회자가 이 그룹에서 작성자에게 지정한 호칭. 지정하지 않았으면 null.
             * 이름 편집 화면의 입력 기본값으로 쓴다 — {@code displayName}은 닉네임으로 대체된 값이라
             * 호칭 유무를 구분할 수 없다.
             */
            String alias,
            /** 화면에 표시할 이름. 호칭이 있으면 호칭, 없으면 닉네임. */
            String displayName,
            String profileImageUrl
    ) {

        /** 호칭이 붙을 수 없는 경로(본인 댓글)에서 쓴다. 자기 자신에게는 호칭을 지정할 수 없다. */
        public static Writer withoutAlias(Long userId, String nickname, String profileImageUrl) {
            return new Writer(userId, null, nickname, null, nickname, profileImageUrl);
        }
    }

    public static CommentResponse of(VideoComment comment, Long groupId, Writer writer, Long currentUserId) {
        return new CommentResponse(
                comment.getId(),
                groupId,
                comment.getContent(),
                writer,
                comment.isWrittenBy(currentUserId),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
