package com.example.v_o_server.common.response;

import com.example.v_o_server.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 모든 API 응답을 감싸는 공통 응답 객체.
 *
 * @param <T> 응답 데이터 타입
 */
@Schema(description = "공통 API 응답")
public record ApiResponse<T>(
        @Schema(description = "요청 성공 여부", example = "true")
        boolean success,

        @Schema(description = "응답 코드", example = "OK")
        String code,

        @Schema(description = "응답 메시지", example = "요청이 성공했습니다.")
        String message,

        @Schema(description = "응답 데이터")
        T data,

        @Schema(description = "필드 단위 검증 오류 목록 (오류 시)")
        List<FieldError> errors,

        @Schema(description = "응답 생성 시각")
        LocalDateTime timestamp
) {

    /* -------------------- 성공 -------------------- */

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", "요청이 성공했습니다.", data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "OK", message, data, null, LocalDateTime.now());
    }

    /** 반환 데이터가 없는 성공 응답 (예: 생성/삭제). */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, "OK", "요청이 성공했습니다.", null, null, LocalDateTime.now());
    }

    /* -------------------- 실패 -------------------- */

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null, null, LocalDateTime.now());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.getCode(), message, null, null, LocalDateTime.now());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, List<FieldError> errors) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null, errors, LocalDateTime.now());
    }

    /**
     * 필드 단위 검증 오류.
     */
    @Schema(description = "필드 검증 오류")
    public record FieldError(
            @Schema(description = "오류 필드명", example = "email")
            String field,

            @Schema(description = "거부된 값", example = "not-an-email")
            String value,

            @Schema(description = "오류 사유", example = "올바른 이메일 형식이 아닙니다.")
            String reason
    ) {
    }
}
