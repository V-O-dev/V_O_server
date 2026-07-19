package com.example.v_o_server.domain.user.repository;

import com.example.v_o_server.domain.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}