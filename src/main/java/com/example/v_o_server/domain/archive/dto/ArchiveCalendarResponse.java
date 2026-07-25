package com.example.v_o_server.domain.archive.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "월별 캘린더 Dot 응답")
public record ArchiveCalendarResponse(
        @Schema(description = "연도", example = "2026")
        int year,

        @Schema(description = "월", example = "7")
        int month,

        @Schema(description = "기록이 있는 날짜(일) 목록", example = "[1, 3, 7]")
        List<Integer> recordDates
) {
}
