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
            String profileImageUrl
    ) {
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
