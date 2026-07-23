package com.example.v_o_server.domain.group.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.common.security.CurrentUserProvider;
import com.example.v_o_server.domain.group.dto.InviteInfoResponse;
import com.example.v_o_server.domain.group.service.GroupInviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "GroupInvite", description = "그룹 초대 API")
@RestController
@RequestMapping("/invites")
@RequiredArgsConstructor
public class GroupInviteController {

    /** 같은 코드의 QR은 항상 같은 이미지라 클라이언트가 캐시해도 안전하다. */
    private static final Duration QR_CACHE_TTL = Duration.ofHours(1);

    private final GroupInviteService groupInviteService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "초대 정보 조회·검증", description = "초대 코드의 유효성을 확인하고 대상 그룹 정보를 반환합니다.")
    @GetMapping("/{code}")
    public ApiResponse<InviteInfoResponse> getInviteInfo(@PathVariable String code) {
        // 인증된 사용자만 초대 정보를 열람할 수 있다.
        currentUserProvider.getCurrentUserId();
        return ApiResponse.success(groupInviteService.getInviteInfo(code));
    }

    @Operation(summary = "초대 코드 QR 이미지",
            description = "초대 링크를 인코딩한 QR PNG를 반환합니다. size는 128~1024, 기본 512입니다.")
    @GetMapping(value = "/{code}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getInviteQrImage(@PathVariable String code,
                                                   @RequestParam(required = false) Integer size) {
        currentUserProvider.getCurrentUserId();
        byte[] png = groupInviteService.getInviteQrImage(code, size);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(QR_CACHE_TTL).cachePrivate())
                .body(png);
    }
}
