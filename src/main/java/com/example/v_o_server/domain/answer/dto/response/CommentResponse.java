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
            String nickname,
            /** 이 영상이 속한 그룹에서 조회자가 작성자에게 지정한 호칭이 있으면 그 호칭, 없으면 닉네임. */
            String displayName,
            String profileImageUrl
    ) {

        /** 호칭이 없는 경우 — 본인 댓글처럼 호칭이 붙을 수 없는 경로에서 쓴다. */
        public static Writer withoutAlias(Long userId, String nickname, String profileImageUrl) {
            return new Writer(userId, nickname, nickname, profileImageUrl);
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
