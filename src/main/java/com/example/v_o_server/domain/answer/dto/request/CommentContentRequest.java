package com.example.v_o_server.domain.answer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 댓글 등록/수정 공용 요청.
 *
 * <p>공백/길이 검증은 Bean Validation(@Valid) 대신 서비스 레이어에서 수행한다.
 * 명세상 COMMENT_BLANK(V006)/COMMENT_TOO_LONG(V005) 전용 코드를 반환해야 하는데,
 * @Valid가 먼저 걸리면 공통 코드(C001)로 응답돼 코드가 뭉개지기 때문.</p>
 */
public record CommentContentRequest(
        @Schema(description = "댓글 내용 (공백 제외 1자 이상, 최대 100자)", example = "오늘 영상 너무 웃겨요")
        String content
) {
}
