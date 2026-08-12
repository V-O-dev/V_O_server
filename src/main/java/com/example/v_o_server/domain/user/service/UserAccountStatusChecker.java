package com.example.v_o_server.domain.user.service;

import com.example.v_o_server.common.security.AccountStatusChecker;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * users 테이블의 계정 상태로 판단한다.
 *
 * <p>인증 요청마다 PK 단건 조회가 한 번 추가되지만, DB를 진실의 원천으로 두면
 * 재로그인 시 캐시를 되돌리는 등의 동기화 실수가 생기지 않는다.</p>
 */
@Component
@RequiredArgsConstructor
public class UserAccountStatusChecker implements AccountStatusChecker {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(Long userId) {
        return userRepository.findById(userId)
                .map(user -> !user.isWithdrawn())
                // 계정이 아예 없으면(하드 삭제 등) 통과시키지 않는다.
                .orElse(false);
    }
}
