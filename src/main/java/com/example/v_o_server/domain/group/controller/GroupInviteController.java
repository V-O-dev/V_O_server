package com.example.v_o_server.domain.group.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.common.security.CurrentUserProvider;
import com.example.v_o_server.domain.group.dto.InviteInfoResponse;
import com.example.v_o_server.domain.group.service.GroupInviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "GroupInvite", description = "그룹 초대 API")
@RestController
@RequestMapping("/invites")
@RequiredArgsConstructor
public class GroupInviteController {

    private final GroupInviteService groupInviteService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "초대 정보 조회·검증", description = "초대 코드의 유효성을 확인하고 대상 그룹 정보를 반환합니다.")
    @GetMapping("/{code}")
    public ApiResponse<InviteInfoResponse> getInviteInfo(@PathVariable String code) {
        // 인증된 사용자만 초대 정보를 열람할 수 있다.
        currentUserProvider.getCurrentUserId();
        return ApiResponse.success(groupInviteService.getInviteInfo(code));
    }
}
