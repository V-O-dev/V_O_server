package com.example.v_o_server.common.exception;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.common.response.ApiResponse.FieldError;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * 전역 예외 처리기. 모든 예외를 {@link ApiResponse} 형태로 통일한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException: {} - {}", errorCode.getCode(), e.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode, e.getMessage()));
    }

    /**
     * @Valid 바디 검증 실패.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        List<FieldError> errors = toFieldErrors(e.getBindingResult());
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, errors));
    }

    /**
     * 쿼리 파라미터/경로 변수 검증 실패 (@Validated + @NotBlank/@Min 등).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        List<FieldError> errors = e.getConstraintViolations().stream()
                .map(violation -> new FieldError(
                        lastPathNode(violation.getPropertyPath()),
                        violation.getInvalidValue() == null ? "" : violation.getInvalidValue().toString(),
                        violation.getMessage()))
                .toList();
        log.warn("Parameter validation failed: {}", errors);
        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, errors));
    }

    /**
     * 필수 요청 파라미터 누락.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        return ResponseEntity.status(ErrorCode.MISSING_REQUEST_PARAMETER.getStatus())
                .body(ApiResponse.error(ErrorCode.MISSING_REQUEST_PARAMETER,
                        "필수 파라미터 '" + e.getParameterName() + "'가 누락되었습니다."));
    }

    /**
     * 파라미터 타입 불일치.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(ErrorCode.INVALID_TYPE_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_TYPE_VALUE,
                        "파라미터 '" + e.getName() + "'의 타입이 올바르지 않습니다."));
    }

    /**
     * 요청 바디 파싱 실패 (JSON 형식 오류, enum에 없는 값 등).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Malformed request body: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE));
    }

    /**
     * 지원하지 않는 HTTP 메서드.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED));
    }

    /**
     * 접근 거부 (Spring Security).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(ErrorCode.ACCESS_DENIED.getStatus())
                .body(ApiResponse.error(ErrorCode.ACCESS_DENIED));
    }

    /**
     * 그 외 모든 예외 (최종 방어선).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    /** 검증 대상 경로("method.arg")에서 마지막 노드만 필드명으로 사용한다. */
    private String lastPathNode(Path propertyPath) {
        String last = "";
        for (Path.Node node : propertyPath) {
            last = node.getName();
        }
        return last;
    }

    private List<FieldError> toFieldErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(fe -> new FieldError(
                        fe.getField(),
                        fe.getRejectedValue() == null ? "" : fe.getRejectedValue().toString(),
                        fe.getDefaultMessage()))
                .toList();
    }
}
