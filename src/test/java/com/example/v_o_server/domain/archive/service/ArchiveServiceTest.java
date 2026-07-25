package com.example.v_o_server.domain.archive.service;

import static com.example.v_o_server.domain.group.GroupTestFixtures.group;
import static com.example.v_o_server.domain.group.GroupTestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.v_o_server.domain.archive.dto.ArchiveCalendarResponse;
import com.example.v_o_server.domain.archive.dto.ArchiveDailyResponse;
import com.example.v_o_server.domain.archive.entity.ArchiveEntry;
import com.example.v_o_server.domain.archive.repository.ArchiveEntryRepository;
import com.example.v_o_server.domain.answer.entity.Video;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArchiveService")
class ArchiveServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long GROUP_ID = 100L;

    @Mock
    private ArchiveEntryRepository archiveEntryRepository;

    @InjectMocks
    private ArchiveService archiveService;

    @Test
    @DisplayName("캘린더 조회는 해당 월의 내 기록 날짜(일)만 반환한다")
    void returnsRecordDaysOfMonth() {
        given(archiveEntryRepository.findRecordDates(
                USER_ID, GROUP_ID, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .willReturn(List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15)));

        ArchiveCalendarResponse response = archiveService.getCalendar(USER_ID, GROUP_ID, 2026, 7);

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(7);
        assertThat(response.recordDates()).containsExactly(1, 15);
    }

    @Test
    @DisplayName("기록이 없는 달은 빈 배열을 반환한다")
    void returnsEmptyWhenNoRecords() {
        given(archiveEntryRepository.findRecordDates(
                USER_ID, GROUP_ID, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
                .willReturn(List.of());

        ArchiveCalendarResponse response = archiveService.getCalendar(USER_ID, GROUP_ID, 2026, 2);

        assertThat(response.recordDates()).isEmpty();
    }

    @Test
    @DisplayName("과거 본인 기록 조회는 현재 ACTIVE 그룹이나 멤버십을 요구하지 않는다")
    void doesNotRequireActiveGroupMembership() {
        given(archiveEntryRepository.findRecordDates(
                USER_ID, GROUP_ID, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .willReturn(List.of(LocalDate.of(2026, 7, 17)));

        ArchiveCalendarResponse response = archiveService.getCalendar(USER_ID, GROUP_ID, 2026, 7);

        assertThat(response.recordDates()).containsExactly(17);
    }

    @Test
    @DisplayName("일자별 조회는 기록이 없으면 빈 목록을 반환한다")
    void returnsEmptyDailyRecords() {
        LocalDate date = LocalDate.of(2026, 7, 17);
        given(archiveEntryRepository.findByUserIdAndGroupIdAndRecordDateOrderByIdAsc(USER_ID, GROUP_ID, date))
                .willReturn(List.of());

        ArchiveDailyResponse response = archiveService.getDailyRecords(USER_ID, GROUP_ID, date);

        assertThat(response.records()).isEmpty();
    }

    @Test
    @DisplayName("일자별 조회는 ID 오름차순 기록의 스냅샷과 영상 썸네일을 카드로 반환한다")
    void returnsDailyRecordCardsInRepositoryOrder() {
        LocalDate date = LocalDate.of(2026, 7, 17);
        ArchiveEntry first = archiveEntry(3L, 10L, date, "첫 번째 질문", "가족", "FAMILY",
                "https://cdn.example.com/thumbnails/10.jpg");
        ArchiveEntry second = archiveEntry(7L, 20L, date, "두 번째 질문", "친구", "FRIEND",
                null);
        given(archiveEntryRepository.findByUserIdAndGroupIdAndRecordDateOrderByIdAsc(
                USER_ID, GROUP_ID, date)).willReturn(List.of(first, second));

        ArchiveDailyResponse response = archiveService.getDailyRecords(USER_ID, GROUP_ID, date);

        assertThat(response.records()).hasSize(2);
        assertThat(response.records().get(0).archiveId()).isEqualTo(3L);
        assertThat(response.records().get(0).questionContent()).isEqualTo("첫 번째 질문");
        assertThat(response.records().get(0).groupName()).isEqualTo("가족");
        assertThat(response.records().get(0).groupTheme()).isEqualTo("FAMILY");
        assertThat(response.records().get(0).videoId()).isEqualTo(10L);
        assertThat(response.records().get(0).thumbnailUrl())
                .isEqualTo("https://cdn.example.com/thumbnails/10.jpg");
        assertThat(response.records().get(1).archiveId()).isEqualTo(7L);
        assertThat(response.records().get(1).thumbnailUrl()).isNull();
    }

    private ArchiveEntry archiveEntry(Long archiveId, Long videoId, LocalDate recordDate,
                                      String questionContent, String groupName, String groupTheme,
                                      String thumbnailUrl) {
        Video video = Video.builder().thumbnailUrl(thumbnailUrl).build();
        ReflectionTestUtils.setField(video, "id", videoId);
        ArchiveEntry entry = ArchiveEntry.builder()
                .user(user(USER_ID))
                .group(group(GROUP_ID, user(USER_ID), 15))
                .video(video)
                .recordDate(recordDate)
                .questionContentSnapshot(questionContent)
                .groupNameSnapshot(groupName)
                .groupThemeSnapshot(groupTheme)
                .build();
        ReflectionTestUtils.setField(entry, "id", archiveId);
        return entry;
    }
}
