package com.example.v_o_server.domain.group.repository;

import com.example.v_o_server.domain.group.entity.GroupInvite;
import com.example.v_o_server.domain.group.entity.InviteStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {

    Optional<GroupInvite> findByInviteCode(String inviteCode);

    /**
     * 아직 쓸 수 있는 초대 중 가장 늦게 만료되는 것을 조회한다.
     * 코드 재발급 대신 기존 코드를 그대로 재사용하기 위한 조회다.
     */
    Optional<GroupInvite> findFirstByGroupIdAndStatusAndExpiresAtAfterOrderByExpiresAtDesc(
            Long groupId, InviteStatus status, LocalDateTime now);

    /**
     * 그룹 삭제 시 해당 그룹의 살아 있는 초대를 전부 무효화한다.
     *
     * @return 무효화된 초대 수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update GroupInvite i
               set i.status = com.example.v_o_server.domain.group.entity.InviteStatus.REVOKED
             where i.group.id = :groupId
               and i.status = com.example.v_o_server.domain.group.entity.InviteStatus.ACTIVE
            """)
    int revokeAllActiveInvites(@Param("groupId") Long groupId);
}
