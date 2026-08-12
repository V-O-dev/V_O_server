package com.example.v_o_server.domain.feed.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.domain.feed.dto.FeedResponse;
import com.example.v_o_server.domain.feed.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Feed", description = "그룹 영상 피드 API")
@RestController
@RequestMapping("/api/v1/groups/{groupId}/feed")
@RequiredArgsConstructor
@Validated
public class FeedController {

    private final FeedService feedService;

    @Operation(
            summary = "그룹 영상 피드 조회",
            description = "해당 날짜의 ACTIVE 영상 피드를 최신순으로 제공합니다. "
                    + "조회자가 답변을 업로드하지 않았다면 unlocked=false이며, 클라이언트는 items의 영상을 블러 처리해야 합니다."
    )
    @GetMapping
    public ApiResponse<FeedResponse> getFeed(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long groupId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate serviceDate,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.") int size
    ) {
        LocalDate targetDate = serviceDate == null ? LocalDate.now() : serviceDate;
        return ApiResponse.success(feedService.getFeed(userId, groupId, targetDate, page, size));
    }
}
