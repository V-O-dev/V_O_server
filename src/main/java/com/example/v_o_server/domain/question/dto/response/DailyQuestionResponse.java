package com.example.v_o_server.domain.question.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "오늘의 질문 응답")
public record DailyQuestionResponse(
        @Schema(description = "그룹별 오늘의 질문 ID", example = "1")
        Long groupDailyQuestionId,

        @Schema(description = "질문 ID", example = "10")
        Long questionId,

        @Schema(description = "질문 내용", example = "만약 1억이 생긴다면 가장 먼저 하고 싶은 일은?")
        String content,

        @Schema(description = "촬영 제한 시간(ms)", example = "10000")
        Integer answerTimeLimitMs,

        @Schema(description = "질문 제공일")
        LocalDate serviceDate,

        @Schema(description = "질문 만료 일시")
        LocalDateTime expiresAt
) {
}
