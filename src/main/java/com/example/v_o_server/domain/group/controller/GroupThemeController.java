package com.example.v_o_server.domain.group.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.domain.group.dto.GroupThemeResponse;
import com.example.v_o_server.domain.group.service.GroupThemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "GroupTheme", description = "그룹 테마 API")
@RestController
@RequestMapping("/api/v1/group-themes")
@RequiredArgsConstructor
public class GroupThemeController {

    private final GroupThemeService groupThemeService;

    @Operation(summary = "그룹 테마 목록 조회", description = "활성화된 그룹 테마를 정렬 순서대로 조회합니다.")
    @GetMapping
    public ApiResponse<List<GroupThemeResponse>> getThemes() {
        return ApiResponse.success(groupThemeService.getActiveThemes());
    }
}
