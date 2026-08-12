package com.example.v_o_server.common.security;

/**
 * 인증 시점에 계정이 아직 사용 가능한 상태인지 확인한다.
 *
 * <p>토큰이 유효해도 탈퇴한 계정이면 API를 쓸 수 없어야 한다. 탈퇴 시 refreshToken을 모두 폐기하고
 * 현재 accessToken을 블랙리스트에 넣지만, <b>다른 기기에 남아있는 accessToken</b>은 만료 전까지
 * 그대로 통과하므로 여기서 한 번 더 막는다.</p>
 *
 * <p>{@code common}이 {@code domain}에 직접 의존하지 않도록 인터페이스로 분리했다.</p>
 */
public interface AccountStatusChecker {

    /** 해당 사용자가 API를 사용할 수 있는 상태면 true. */
    boolean isActive(Long userId);
}
