package com.example.v_o_server.domain.group.repository;

import com.example.v_o_server.domain.group.entity.GroupStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivateGroupRepository extends JpaRepository<PrivateGroup, Long> {

    boolean existsByOwnerAndStatus(User owner, GroupStatus status);
}
