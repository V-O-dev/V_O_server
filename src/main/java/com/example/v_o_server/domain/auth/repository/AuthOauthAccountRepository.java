package com.example.v_o_server.domain.auth.repository;

import com.example.v_o_server.domain.auth.entity.AuthOauthAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthOauthAccountRepository extends JpaRepository<AuthOauthAccount, Long> {

    Optional<AuthOauthAccount> findByUserId(Long userId);
}