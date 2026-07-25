package com.example.v_o_server.domain.archive.service;

import com.example.v_o_server.domain.archive.dto.ArchiveCalendarResponse;
import com.example.v_o_server.domain.archive.dto.ArchiveDailyResponse;
import com.example.v_o_server.domain.archive.dto.ArchiveRecordResponse;
import com.example.v_o_server.domain.archive.repository.ArchiveEntryRepository;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 나의 달력(아카이브) 조회.
 *
 * <p>프라이버시: 결과는 항상 현재 사용자 소유 레코드로 한정되며, 나아가 <b>내가 현재 ACTIVE 멤버인 그룹</b>의
 * 기록으로만 제한한다(A-1). 탈퇴·강퇴한 그룹의 과거 기록은 달력에서 보이지 않는다.</p>
 *
 * <p>{@code groupId}는 선택 값이다. 없으면 내 모든 ACTIVE 그룹을 가로질러 통합 조회하고(기능명세서·Figma의
 * "나의 달력"), 있으면 그 그룹만 본다. 단 그 그룹이 내 ACTIVE 그룹이 아니면(예: 나간 그룹) 결과는 비어 있다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArchiveService {

    private final ArchiveEntryRepository archiveEntryRepository;
    private final GroupMemberRepository groupMemberRepository;

    /** A1 캘린더(월별 Dot) 조회. groupId가 null이면 내 모든 ACTIVE 그룹 통합. */
    public ArchiveCalendarResponse getCalendar(Long userId, Long groupId, int year, int month) {
        List<Long> groupIds = resolveGroupIds(userId, groupId);
        if (groupIds.isEmpty()) {
            return new ArchiveCalendarResponse(year, month, List.of());
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        List<Integer> days = archiveEntryRepository
                .findRecordDatesInGroups(userId, groupIds, yearMonth.atDay(1), yearMonth.atEndOfMonth())
                .stream()
                .map(LocalDate::getDayOfMonth)
                .toList();
        return new ArchiveCalendarResponse(year, month, days);
    }

    /** A2 일자별 기록 조회 — 기록이 없으면 빈 목록(정상 응답). groupId가 null이면 내 모든 ACTIVE 그룹 통합. */
    public ArchiveDailyResponse getDailyRecords(Long userId, Long groupId, LocalDate date) {
        List<Long> groupIds = resolveGroupIds(userId, groupId);
        if (groupIds.isEmpty()) {
            return new ArchiveDailyResponse(List.of());
        }

        List<ArchiveRecordResponse> records = archiveEntryRepository
                .findDailyRecordsInGroups(userId, groupIds, date).stream()
                .map(ArchiveRecordResponse::from)
                .toList();
        return new ArchiveDailyResponse(records);
    }

    /**
     * A-1: 조회 범위를 "내가 현재 ACTIVE 멤버인 그룹"으로 좁힌다.
     *
     * <ul>
     *   <li>groupId == null → 내 모든 ACTIVE 그룹.</li>
     *   <li>groupId != null 이고 그 그룹이 내 ACTIVE 그룹 → 그 그룹만.</li>
     *   <li>groupId != null 이지만 내 ACTIVE 그룹이 아님(나간·강퇴·미가입) → 빈 범위 → 빈 결과.</li>
     * </ul>
     */
    private List<Long> resolveGroupIds(Long userId, Long groupId) {
        List<Long> activeGroupIds = groupMemberRepository.findActiveGroupIds(userId);
        if (groupId == null) {
            return activeGroupIds;
        }
        return activeGroupIds.contains(groupId) ? List.of(groupId) : List.of();
    }
}
