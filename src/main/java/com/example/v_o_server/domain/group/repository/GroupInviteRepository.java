package com.example.v_o_server.domain.group.repository;

import com.example.v_o_server.domain.group.entity.GroupInvite;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {

    Optional<GroupInvite> findByInviteCode(String inviteCode);
}
