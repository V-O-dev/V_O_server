package com.example.v_o_server.domain.archive.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "일자별 기록 조회 응답")
public record ArchiveDailyResponse(
        @Schema(description = "해당 일자의 기록 목록 (없으면 빈 배열)")
        List<ArchiveRecordResponse> records
) {
}
