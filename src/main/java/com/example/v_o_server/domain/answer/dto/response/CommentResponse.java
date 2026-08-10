package com.example.v_o_server.domain.answer.dto.response;

import com.example.v_o_server.domain.answer.entity.VideoComment;
import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
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
            /** 이 영상이 속한 그룹에서 조회자가 작성자에게 지정한 호칭이 있으면 그 호칭, 없으면 닉네임. */
            String displayName,
            String profileImageUrl
    ) {

        /** 호칭이 붙을 수 없는 경로(본인 댓글)에서 쓴다. 자기 자신에게는 호칭을 지정할 수 없다. */
        public static Writer withoutAlias(Long userId, String nickname, String profileImageUrl) {
            return new Writer(userId, null, nickname, nickname, profileImageUrl);
        }
    }

    public static CommentResponse of(VideoComment comment, Writer writer, Long currentUserId) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                writer,
                comment.isWrittenBy(currentUserId),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
