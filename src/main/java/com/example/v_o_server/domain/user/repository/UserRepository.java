package com.example.v_o_server.domain.user.repository;

import com.example.v_o_server.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 유저 도메인은 아직 미구현이나, 그룹 도메인이 현재 사용자 엔티티 참조를 해석하기 위해
 * 리포지토리만 우선 도입한다. (서비스/컨트롤러는 인증 작업에서 추가)
 */
public interface UserRepository extends JpaRepository<User, Long> {
}
