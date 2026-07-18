package com.example.v_o_server.domain.group.repository;

import com.example.v_o_server.domain.group.entity.GroupStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrivateGroupRepository extends JpaRepository<PrivateGroup, Long> {

    Optional<PrivateGroup> findByIdAndStatus(Long id, GroupStatus status);

    /**
     * 가입 처리 시 정원 초과를 막기 위해 그룹 행을 비관적 쓰기 락으로 조회한다.
     * 반드시 트랜잭션 안에서 호출해야 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from PrivateGroup g where g.id = :id and g.status = :status")
    Optional<PrivateGroup> findByIdAndStatusForUpdate(@Param("id") Long id, @Param("status") GroupStatus status);
}
