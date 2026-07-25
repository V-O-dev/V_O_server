package com.example.v_o_server.domain.archive.repository;

import com.example.v_o_server.domain.archive.entity.ArchiveEntry;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 아카이브 조회는 항상 본인(userId) 소유 레코드로 한정한다(기획안 9.2 — 타인 아카이브 열람 불가).
 * 따라서 모든 조회 메서드는 userId를 필수 인자로 받는다.
 *
 * <p>조회 범위 그룹은 서비스가 "내가 현재 ACTIVE 멤버인 그룹"으로 결정해 {@code groupIds}로 넘긴다
 * (A-1 정책). groupIds가 비어 있을 때는 서비스가 쿼리 자체를 건너뛰므로, 여기서는 비어 있지 않다고 가정한다.</p>
 */
public interface ArchiveEntryRepository extends JpaRepository<ArchiveEntry, Long> {

    /** 주어진 그룹들·월의 내 기록 날짜 목록 (달력 Dot용). */
    @Query("""
            select distinct e.recordDate from ArchiveEntry e
            where e.user.id = :userId
              and e.group.id in :groupIds
              and e.recordDate between :startDate and :endDate
            order by e.recordDate asc
            """)
    List<LocalDate> findRecordDatesInGroups(@Param("userId") Long userId,
                                            @Param("groupIds") Collection<Long> groupIds,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /** 주어진 그룹들·일자의 내 기록 카드 목록. */
    @EntityGraph(attributePaths = "video")
    @Query("""
            select e from ArchiveEntry e
            where e.user.id = :userId
              and e.group.id in :groupIds
              and e.recordDate = :recordDate
            order by e.id asc
            """)
    List<ArchiveEntry> findDailyRecordsInGroups(@Param("userId") Long userId,
                                                @Param("groupIds") Collection<Long> groupIds,
                                                @Param("recordDate") LocalDate recordDate);
}
