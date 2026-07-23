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
    OAUTH_INVALID_CODE(HttpStatus.UNAUTHORIZED, "A003", "OAuth 인가 코드 검증에 실패했습니다."),
    OAUTH_UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "A004", "지원하지 않는 OAuth provider입니다."),
    OAUTH_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "A005", "OAuth provider 통신 중 오류가 발생했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A006", "유효하지 않은 refresh token입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A007", "refresh token이 만료되었습니다."),
    WITHDRAW_OWNER_EXISTS(HttpStatus.CONFLICT, "A008", "방장으로 있는 그룹이 남아있어 탈퇴할 수 없습니다."),
    /** 로깅/추적 태그 용도. 정책상 클라이언트로 던지지 않고 탈퇴 자체는 롤백하지 않는다. */
    OAUTH_UNLINK_FAILED(HttpStatus.BAD_GATEWAY, "A009", "OAuth provider unlink에 실패했습니다."),

    // 사용자
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "U002", "프로필을 찾을 수 없습니다."),
    NICKNAME_BLANK(HttpStatus.BAD_REQUEST, "U003", "공백을 제외한 한 글자 이상의 문자를 입력해주세요."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "U004", "이름은 최대 15자까지 입력 가능합니다."),
    NICKNAME_INVALID_CHAR(HttpStatus.BAD_REQUEST, "U005", "특수문자 및 이모지는 포함할 수 없습니다."),
    UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "U006", "jpg, jpeg, png 파일만 업로드 가능합니다."),
    IMAGE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "U007", "파일 크기는 최대 5MB를 넘을 수 없습니다."),
    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "U008", "업로드할 이미지를 선택해주세요."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "U009", "이미지 업로드에 실패했습니다. 다시 시도해주세요."),
    IMAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "U010", "이미지 삭제에 실패했습니다. 다시 시도해주세요."),
    DEVICE_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "U011", "디바이스 토큰이 필요합니다."),
    INVALID_PLATFORM(HttpStatus.BAD_REQUEST, "U012", "유효하지 않은 플랫폼 값입니다."),

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
