package com.example.v_o_server.domain.archive.dto;

import com.example.v_o_server.domain.archive.entity.ArchiveEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "나의 달력 기록 카드")
public record ArchiveRecordResponse(
        @Schema(description = "아카이브 항목 ID", example = "1")
        Long archiveId,

        @Schema(description = "기록 날짜")
        LocalDate recordDate,

        @Schema(description = "질문 내용 스냅샷", example = "오늘 가장 기뻤던 순간은?")
        String questionContent,

        @Schema(description = "그룹명 스냅샷", example = "우리 가족")
        String groupName,

        @Schema(description = "그룹 테마 스냅샷", example = "FAMILY")
        String groupTheme,

        @Schema(description = "영상 ID", example = "10")
        Long videoId,

        @Schema(description = "영상 썸네일 URL (없으면 null)",
                example = "https://cdn.example.com/thumbnails/10.jpg", nullable = true)
        String thumbnailUrl
) {
    public static ArchiveRecordResponse from(ArchiveEntry entry) {
        return new ArchiveRecordResponse(
                entry.getId(),
                entry.getRecordDate(),
                entry.getQuestionContentSnapshot(),
                entry.getGroupNameSnapshot(),
                entry.getGroupThemeSnapshot(),
                entry.getVideo().getId(),
                entry.getVideo().getThumbnailUrl());
    }
}
