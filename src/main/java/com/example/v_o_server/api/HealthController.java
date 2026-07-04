package com.example.v_o_server.api;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 스캐폴딩 검증용 샘플 컨트롤러. 실제 도메인 구현 시 삭제하거나 대체하세요.
 */
@Tag(name = "Health", description = "헬스 체크 및 응답 포맷 예시")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Operation(summary = "헬스 체크", description = "서버 상태와 공통 응답 포맷을 확인합니다.")
    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP"));
    }

    @Operation(summary = "에러 응답 예시", description = "공통 에러 응답 포맷을 확인합니다.")
    @GetMapping("/error")
    public ApiResponse<Void> error() {
        throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND);
    }
}
