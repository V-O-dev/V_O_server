package com.example.v_o_server.domain.archive.repository;

import com.example.v_o_server.domain.archive.entity.ArchiveEntry;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 아카이브 조회는 항상 본인(userId) 소유 레코드로 한정한다(기획안 9.2 — 타인 아카이브 열람 불가).
 * 따라서 모든 조회 메서드는 userId를 필수 인자로 받는다.
 */
public interface ArchiveEntryRepository extends JpaRepository<ArchiveEntry, Long> {

    /** 특정 그룹·월의 내 기록 날짜 목록 (달력 Dot용). */
    @Query("""
            select distinct e.recordDate from ArchiveEntry e
            where e.user.id = :userId
              and e.group.id = :groupId
              and e.recordDate between :startDate and :endDate
            order by e.recordDate asc
            """)
    List<LocalDate> findRecordDates(@Param("userId") Long userId,
                                    @Param("groupId") Long groupId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    /** 특정 그룹·일자의 내 기록 카드 목록. */
    List<ArchiveEntry> findByUserIdAndGroupIdAndRecordDate(Long userId, Long groupId, LocalDate recordDate);
}
