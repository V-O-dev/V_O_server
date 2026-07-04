package com.example.v_o_server.common.exception;

import lombok.Getter;

/**
 * 비즈니스 로직에서 발생하는 예외의 최상위 타입.
 * {@link ErrorCode}를 담아 전역 핸들러에서 일관되게 처리한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
