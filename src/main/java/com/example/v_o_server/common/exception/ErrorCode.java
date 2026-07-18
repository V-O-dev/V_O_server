package com.example.v_o_server.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역 에러 코드 정의.
 *
 * <p>code 접두사 규칙: C=공통, A=인증/인가, U=user, G=group.</p>
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

    // 사용자
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),

    // 그룹
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "G001", "그룹을 찾을 수 없습니다."),
    GROUP_NAME_DUPLICATED(HttpStatus.CONFLICT, "G002", "이미 사용 중인 그룹명입니다."),
    NOT_GROUP_MEMBER(HttpStatus.FORBIDDEN, "G003", "그룹 멤버가 아닙니다."),
    NOT_GROUP_OWNER(HttpStatus.FORBIDDEN, "G004", "그룹 방장만 수행할 수 있습니다."),
    GROUP_FULL(HttpStatus.CONFLICT, "G005", "그룹 정원이 가득 찼습니다."),
    INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "G006", "초대 정보를 찾을 수 없습니다."),
    INVITE_EXPIRED(HttpStatus.GONE, "G007", "만료된 초대 코드입니다."),
    ALREADY_GROUP_MEMBER(HttpStatus.CONFLICT, "G008", "이미 가입된 그룹입니다."),
    OWNER_CANNOT_LEAVE(HttpStatus.CONFLICT, "G009", "방장은 권한을 위임한 뒤 나갈 수 있습니다."),
    CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST, "G010", "자기 자신을 강제 퇴장시킬 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "G011", "그룹 멤버를 찾을 수 없습니다."),
    THEME_NOT_FOUND(HttpStatus.NOT_FOUND, "G012", "그룹 테마를 찾을 수 없습니다."),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "G013", "알림 시간 범위가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
