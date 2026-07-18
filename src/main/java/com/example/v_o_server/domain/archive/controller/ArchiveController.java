package com.example.v_o_server.domain.archive.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.common.security.CurrentUserProvider;
import com.example.v_o_server.domain.archive.dto.ArchiveCalendarResponse;
import com.example.v_o_server.domain.archive.dto.ArchiveDailyResponse;
import com.example.v_o_server.domain.archive.service.ArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Archive", description = "나의 달력(아카이브) API")
@RestController
@RequestMapping("/archives")
@RequiredArgsConstructor
@Validated
public class ArchiveController {

    private final ArchiveService archiveService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "캘린더(월별 Dot) 조회",
            description = "해당 그룹에서 내가 기록을 남긴 날짜 목록을 조회합니다. 본인 기록만 반환됩니다.")
    @GetMapping("/calendar")
    public ApiResponse<ArchiveCalendarResponse> getCalendar(
            @RequestParam Long groupId,
            @RequestParam @Min(value = 2000, message = "연도는 2000 이상이어야 합니다.")
            @Max(value = 2100, message = "연도는 2100 이하여야 합니다.") int year,
            @RequestParam @Min(value = 1, message = "월은 1 이상이어야 합니다.")
            @Max(value = 12, message = "월은 12 이하여야 합니다.") int month) {
        Long userId = currentUserProvider.getCurrentUserId();
        return ApiResponse.success(archiveService.getCalendar(userId, groupId, year, month));
    }

    @Operation(summary = "일자별 기록 조회",
            description = "해당 그룹·날짜의 내 기록 카드를 조회합니다. 기록이 없으면 빈 목록을 반환합니다.")
    @GetMapping("/daily")
    public ApiResponse<ArchiveDailyResponse> getDaily(
            @RequestParam Long groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = currentUserProvider.getCurrentUserId();
        return ApiResponse.success(archiveService.getDailyRecords(userId, groupId, date));
    }
}
