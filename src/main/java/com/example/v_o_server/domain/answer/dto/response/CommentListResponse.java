package com.example.v_o_server.domain.answer.dto.response;

import java.util.List;

public record CommentListResponse(
        List<CommentResponse> comments,
        boolean hasNext
) {
}
