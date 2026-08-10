package com.example.v_o_server.domain.group.repository;

import com.example.v_o_server.domain.group.entity.GroupMemberAlias;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberAliasRepository extends JpaRepository<GroupMemberAlias, Long> {

    /**
     * 특정 viewer가 이 그룹에서 지정한 호칭 중, 대상이 {@code targetUserIds}에 포함된 것만 조회한다.
     *
     * <p>대상 집합으로 반드시 좁힌다 — 나갔다 들어온 멤버의 호칭 행이 계속 쌓이므로
     * "그룹 정원이 15명이니 viewer 기준 전량을 읽어도 된다"는 가정은 성립하지 않는다.</p>
     */
    @Query("""
            select a from GroupMemberAlias a
            where a.group.id = :groupId
              and a.viewer.id = :viewerUserId
              and a.target.id in :targetUserIds
            """)
    List<GroupMemberAlias> findAliases(@Param("groupId") Long groupId,
                                       @Param("viewerUserId") Long viewerUserId,
                                       @Param("targetUserIds") Collection<Long> targetUserIds);

    /**
     * 호칭 설정·변경을 한 문장으로 처리한다.
     *
     * <p>"조회 후 없으면 insert" 방식은 쓰지 않는다. PostgreSQL은 유니크 제약 위반이 flush되는 순간
     * 트랜잭션 전체를 abort 상태로 만들기 때문에, 같은 트랜잭션에서 예외를 잡아 재조회·update 하는
     * 복구가 동작하지 않는다. DB 레벨 원자적 upsert로 경쟁 자체를 없앤다.</p>
     *
     * <p>네이티브 INSERT는 JPA Auditing({@code BaseTimeEntity})을 타지 않으므로
     * {@code created_at}/{@code updated_at}을 SQL에서 직접 채운다.</p>
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            insert into group_member_aliases
                   (group_id, viewer_user_id, target_user_id, alias, created_at, updated_at)
            values (:groupId, :viewerUserId, :targetUserId, :alias, now(), now())
            on conflict (group_id, viewer_user_id, target_user_id)
            do update set alias = excluded.alias, updated_at = now()
            """, nativeQuery = true)
    int upsertAlias(@Param("groupId") Long groupId,
                    @Param("viewerUserId") Long viewerUserId,
                    @Param("targetUserId") Long targetUserId,
                    @Param("alias") String alias);

    /** 호칭 해제. 대상 행이 없어도 0을 반환할 뿐 실패가 아니다(멱등). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from GroupMemberAlias a
            where a.group.id = :groupId
              and a.viewer.id = :viewerUserId
              and a.target.id = :targetUserId
            """)
    int deleteAlias(@Param("groupId") Long groupId,
                    @Param("viewerUserId") Long viewerUserId,
                    @Param("targetUserId") Long targetUserId);
}
