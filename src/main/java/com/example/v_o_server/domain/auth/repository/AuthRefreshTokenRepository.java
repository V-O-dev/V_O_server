package com.example.v_o_server.domain.auth.repository;

import com.example.v_o_server.domain.auth.entity.AuthRefreshToken;
import com.example.v_o_server.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    Optional<AuthRefreshToken> findByRefreshTokenHash(String refreshTokenHash);

    /** 탈퇴 시 모든 기기의 재발급 수단을 끊기 위해 아직 살아있는 토큰을 전부 가져온다. */
    List<AuthRefreshToken> findAllByUserAndRevokedAtIsNull(User user);
}
