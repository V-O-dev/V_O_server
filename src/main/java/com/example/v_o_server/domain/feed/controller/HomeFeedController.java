package com.example.v_o_server.domain.feed.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.domain.feed.dto.HomeFeedResponse;
import com.example.v_o_server.domain.feed.service.HomeFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Feed", description = "그룹 영상 피드 API")
@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
@Validated
public class HomeFeedController {

    private final HomeFeedService homeFeedService;
    private final Clock clock;

    @Operation(
            summary = "전체 그룹 통합 홈 피드 조회",
            description = "내가 ACTIVE로 속한 모든 그룹의 해당 날짜 ACTIVE 영상을 uploadedAt 최신순 한 타임라인으로 합쳐 제공합니다. "
                    + "잠금은 그룹별로 판단되며(groups[]·items[]의 unlocked), 잠긴 그룹의 영상도 items에 포함되므로 "
                    + "클라이언트가 블러 처리해야 합니다."
    )
    @GetMapping("/home")
    public ApiResponse<HomeFeedResponse> getHomeFeed(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate serviceDate,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.") int size
    ) {
        LocalDate targetDate = serviceDate == null ? LocalDate.now(clock) : serviceDate;
        return ApiResponse.success(homeFeedService.getHomeFeed(userId, targetDate, page, size));
    }
}
