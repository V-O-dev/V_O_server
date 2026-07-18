package com.example.v_o_server.common.security;

/**
 * 현재 요청을 수행하는 사용자의 식별자를 제공하는 이음새(seam).
 *
 * <p>인증(JWT) 도입 전까지는 {@link HeaderCurrentUserProvider}가 임시로 헤더에서 사용자를 식별한다.
 * JWT 도입 시 이 인터페이스의 구현체만 교체하면 되며, 그룹/아카이브 도메인 코드는 바뀌지 않는다.</p>
 */
public interface CurrentUserProvider {

    /**
     * @return 현재 사용자 ID
     * @throws com.example.v_o_server.common.exception.BusinessException 식별할 수 없으면 UNAUTHORIZED
     */
    Long getCurrentUserId();
}
