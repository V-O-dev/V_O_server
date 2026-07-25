package com.example.v_o_server.domain.answer.dto.response;

import com.example.v_o_server.domain.answer.entity.VideoComment;
import java.time.LocalDateTime;

public record CommentUpdateResponse(
        Long commentId,
        String content,
        boolean isMine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CommentUpdateResponse of(VideoComment comment, Long currentUserId) {
        return new CommentUpdateResponse(
                comment.getId(),
                comment.getContent(),
                comment.isWrittenBy(currentUserId),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
