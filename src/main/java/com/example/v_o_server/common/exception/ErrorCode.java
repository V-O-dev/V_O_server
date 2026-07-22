package com.example.v_o_server.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역 에러 코드 정의.
 *
 * <p>code 접두사 규칙: C=공통, 이후 도메인별로 A(auth), U(user) 등을 추가하세요.</p>
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 HTTP 메서드입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 내부 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "요청 파라미터 타입이 올바르지 않습니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "C006", "필수 요청 파라미터가 누락되었습니다."),

    // 인증/인가
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),

    // 그룹
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "G001", "그룹을 찾을 수 없습니다."),
    GROUP_THEME_NOT_SET(HttpStatus.BAD_REQUEST, "G002", "그룹에 테마가 설정되어 있지 않습니다."),

    // 질문
    NO_QUESTION_CANDIDATE(HttpStatus.NOT_FOUND, "Q001", "테마에 해당하는 추천 가능한 질문이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
