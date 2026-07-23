package com.example.v_o_server.domain.answer.dto.response;

public record ReactionResponse(
        Long videoId,
        long likeCount,
        boolean isLiked
) {
}
