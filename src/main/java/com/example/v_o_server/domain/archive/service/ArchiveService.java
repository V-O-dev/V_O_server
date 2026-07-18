package com.example.v_o_server.domain.archive.service;

import com.example.v_o_server.domain.archive.dto.ArchiveCalendarResponse;
import com.example.v_o_server.domain.archive.dto.ArchiveDailyResponse;
import com.example.v_o_server.domain.archive.dto.ArchiveRecordResponse;
import com.example.v_o_server.domain.archive.repository.ArchiveEntryRepository;
import com.example.v_o_server.domain.group.service.GroupAccessGuard;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 나의 달력(아카이브) 조회.
 *
 * <p>프라이버시: groupId는 "그 그룹에서의 내 기록"으로 범위를 좁히는 필터일 뿐이며,
 * 결과는 항상 현재 사용자 소유 레코드로 한정된다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArchiveService {

    private final ArchiveEntryRepository archiveEntryRepository;
    private final GroupAccessGuard accessGuard;

    /** A1 캘린더(월별 Dot) 조회. */
    public ArchiveCalendarResponse getCalendar(Long userId, Long groupId, int year, int month) {
        accessGuard.getActiveGroup(groupId);
        accessGuard.assertMember(groupId, userId);

        YearMonth yearMonth = YearMonth.of(year, month);
        List<Integer> days = archiveEntryRepository
                .findRecordDates(userId, groupId, yearMonth.atDay(1), yearMonth.atEndOfMonth())
                .stream()
                .map(LocalDate::getDayOfMonth)
                .toList();
        return new ArchiveCalendarResponse(year, month, days);
    }

    /** A2 일자별 기록 조회 — 기록이 없으면 빈 목록(정상 응답). */
    public ArchiveDailyResponse getDailyRecords(Long userId, Long groupId, LocalDate date) {
        accessGuard.getActiveGroup(groupId);
        accessGuard.assertMember(groupId, userId);

        List<ArchiveRecordResponse> records = archiveEntryRepository
                .findByUserIdAndGroupIdAndRecordDate(userId, groupId, date).stream()
                .map(ArchiveRecordResponse::from)
                .toList();
        return new ArchiveDailyResponse(records);
    }
}
