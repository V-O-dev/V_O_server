package com.example.v_o_server.domain.group.repository;

import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroupIdAndUserIdAndStatus(Long groupId, Long userId, MemberStatus status);

    /** 재가입 판단을 위해 상태와 무관하게 기존 멤버십 이력을 조회한다. */
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    Optional<GroupMember> findByIdAndGroupIdAndStatus(Long id, Long groupId, MemberStatus status);

    long countByGroupIdAndStatus(Long groupId, MemberStatus status);

    List<GroupMember> findByGroupIdAndStatus(Long groupId, MemberStatus status);

    /**
     * 그룹의 특정 사용자들에 대한 멤버십을 한 번에 조회한다.
     *
     * <p>피드·댓글 응답에 {@code memberId}를 실어 주기 위한 것이다 — 호칭 API 경로가
     * {@code group_members.id}를 요구하므로, 작성자의 userId만으로는 클라이언트가 호출할 수 없다.</p>
     */
    List<GroupMember> findByGroupIdAndStatusAndUserIdIn(Long groupId, MemberStatus status,
                                                        Collection<Long> userIds);

    /** 홈 피드용 — 여러 그룹에 걸친 작성자의 memberId를 한 번에 조회한다. */
    List<GroupMember> findByGroupIdInAndStatusAndUserIdIn(Collection<Long> groupIds, MemberStatus status,
                                                          Collection<Long> userIds);

    /** 현재 사용자가 ACTIVE 멤버로 속한 그룹의 멤버십 목록 (그룹도 ACTIVE인 것만). */
    @Query("""
            select m from GroupMember m
            join fetch m.group g
            where m.user.id = :userId
              and m.status = com.example.v_o_server.domain.group.entity.MemberStatus.ACTIVE
              and g.status = com.example.v_o_server.domain.group.entity.GroupStatus.ACTIVE
            """)
    List<GroupMember> findActiveMembershipsWithGroup(@Param("userId") Long userId);

    /**
     * 현재 사용자가 ACTIVE 멤버인 그룹의 id 목록 (그룹도 ACTIVE인 것만).
     *
     * <p>아카이브 조회 범위를 "내가 지금 속한 그룹"으로 제한하는 데 쓴다.
     * 탈퇴(LEFT)·강제 퇴장(KICKED)한 그룹은 여기서 빠지므로, 그 그룹의 과거 기록은 달력에서 보이지 않는다.</p>
     */
    @Query("""
            select m.group.id from GroupMember m
            where m.user.id = :userId
              and m.status = com.example.v_o_server.domain.group.entity.MemberStatus.ACTIVE
              and m.group.status = com.example.v_o_server.domain.group.entity.GroupStatus.ACTIVE
            """)
    List<Long> findActiveGroupIds(@Param("userId") Long userId);

    /**
     * 그룹 삭제 시 남아 있는 ACTIVE 멤버를 전부 LEFT로 정리한다.
     *
     * <p>강제 퇴장이 아니므로 KICKED가 아닌 LEFT를 쓴다(재가입 차단 대상이 되면 안 된다).
     * 멤버 수가 최대 15명이라 벌크 업데이트 한 번이면 충분하다.</p>
     *
     * @return 정리된 멤버 수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update GroupMember m
               set m.status = com.example.v_o_server.domain.group.entity.MemberStatus.LEFT,
                   m.leftAt = :leftAt
             where m.group.id = :groupId
               and m.status = com.example.v_o_server.domain.group.entity.MemberStatus.ACTIVE
            """)
    int leaveAllActiveMembers(@Param("groupId") Long groupId, @Param("leftAt") LocalDateTime leftAt);

    /** 그룹명 중복 확인 — 현재 사용자의 ACTIVE 그룹 중 동일 이름 존재 여부 (excludeGroupId는 제외). */
    @Query("""
            select count(m) > 0 from GroupMember m
            where m.user.id = :userId
              and m.status = com.example.v_o_server.domain.group.entity.MemberStatus.ACTIVE
              and m.group.status = com.example.v_o_server.domain.group.entity.GroupStatus.ACTIVE
              and m.group.name = :name
              and (:excludeGroupId is null or m.group.id <> :excludeGroupId)
            """)
    boolean existsActiveGroupNameForUser(@Param("userId") Long userId,
                                         @Param("name") String name,
                                         @Param("excludeGroupId") Long excludeGroupId);
}
