package com.example.v_o_server.domain.auth.repository;

import com.example.v_o_server.domain.auth.entity.AuthRefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    Optional<AuthRefreshToken> findByRefreshTokenHash(String refreshTokenHash);
}
