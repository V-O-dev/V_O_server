package com.example.v_o_server.domain.auth.repository;

import com.example.v_o_server.domain.auth.entity.AuthOauthAccount;
import com.example.v_o_server.domain.auth.entity.OauthProvider;
import com.example.v_o_server.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthOauthAccountRepository extends JpaRepository<AuthOauthAccount, Long> {

    Optional<AuthOauthAccount> findByProviderAndProviderUserId(OauthProvider provider, String providerUserId);

    List<AuthOauthAccount> findAllByUser(User user);
}

