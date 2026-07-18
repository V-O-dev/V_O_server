package com.example.v_o_server.common.security;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 인증 도입 전 임시 구현. 요청 헤더 {@code X-User-Id}에서 사용자 ID를 읽는다.
 *
 * <p><b>주의:</b> 이 구현은 헤더 값을 신뢰하므로 개발 단계 전용이다.
 * JWT 인증이 도입되면 토큰 기반 구현체로 교체해야 한다.</p>
 */
@Component
@RequiredArgsConstructor
public class HeaderCurrentUserProvider implements CurrentUserProvider {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final HttpServletRequest request;

    @Override
    public Long getCurrentUserId() {
        String header = request.getHeader(USER_ID_HEADER);
        if (header == null || header.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
