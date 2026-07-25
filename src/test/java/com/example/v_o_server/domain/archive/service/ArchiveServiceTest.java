package com.example.v_o_server.domain.archive.service;

import static com.example.v_o_server.domain.group.GroupTestFixtures.group;
import static com.example.v_o_server.domain.group.GroupTestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.v_o_server.domain.archive.dto.ArchiveCalendarResponse;
import com.example.v_o_server.domain.archive.dto.ArchiveDailyResponse;
import com.example.v_o_server.domain.archive.entity.ArchiveEntry;
import com.example.v_o_server.domain.archive.repository.ArchiveEntryRepository;
import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
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
    private static final Long OTHER_GROUP_ID = 200L;

    @Mock
    private ArchiveEntryRepository archiveEntryRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private ArchiveService archiveService;

    @Test
    @DisplayName("groupId 지정 시 그 그룹이 내 ACTIVE 그룹이면 해당 그룹으로 그 달 기록 날짜를 반환한다")
    void returnsRecordDaysForSpecifiedActiveGroup() {
        given(groupMemberRepository.findActiveGroupIds(USER_ID)).willReturn(List.of(GROUP_ID, OTHER_GROUP_ID));
        given(archiveEntryRepository.findRecordDatesInGroups(
                USER_ID, List.of(GROUP_ID), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .willReturn(List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15)));

        ArchiveCalendarResponse response = archiveService.getCalendar(USER_ID, GROUP_ID, 2026, 7);

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(7);
        assertThat(response.recordDates()).containsExactly(1, 15);
    }

    @Test
    @DisplayName("groupId를 생략하면 내 모든 ACTIVE 그룹을 통합해 조회한다")
    void aggregatesAcrossActiveGroupsWhenGroupIdNull() {
        given(groupMemberRepository.findActiveGroupIds(USER_ID)).willReturn(List.of(GROUP_ID, OTHER_GROUP_ID));
        given(archiveEntryRepository.findRecordDatesInGroups(
                USER_ID, List.of(GROUP_ID, OTHER_GROUP_ID),
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .willReturn(List.of(LocalDate.of(2026, 7, 17)));

        ArchiveCalendarResponse response = archiveService.getCalendar(USER_ID, null, 2026, 7);

        assertThat(response.recordDates()).containsExactly(17);
    }

    @Test
    @DisplayName("A-1: 나간·강퇴한 그룹을 groupId로 지정하면 조회하지 않고 빈 결과를 반환한다")
    void returnsEmptyForGroupIAmNoLongerActiveIn() {
        // 내 ACTIVE 그룹에 GROUP_ID가 없다(나갔거나 강퇴됨).
        given(groupMemberRepository.findActiveGroupIds(USER_ID)).willReturn(List.of(OTHER_GROUP_ID));

        ArchiveCalendarResponse response = archiveService.getCalendar(USER_ID, GROUP_ID, 2026, 7);

        assertThat(response.recordDates()).isEmpty();
        verify(archiveEntryRepository, never()).findRecordDatesInGroups(any(), any(), any(), any());
    }

    @Test
    @DisplayName("속한 ACTIVE 그룹이 하나도 없으면 조회 없이 빈 결과를 반환한다")
    void returnsEmptyWhenNoActiveGroups() {
        given(groupMemberRepository.findActiveGroupIds(USER_ID)).willReturn(List.of());

        ArchiveCalendarResponse response = archiveService.getCalendar(USER_ID, null, 2026, 2);

        assertThat(response.recordDates()).isEmpty();
        verify(archiveEntryRepository, never()).findRecordDatesInGroups(any(), any(), any(), any());
    }

    @Test
    @DisplayName("A-1: 나간·강퇴한 그룹을 groupId로 지정한 일자별 조회는 조회 없이 빈 목록을 반환한다")
    void returnsEmptyDailyForGroupIAmNoLongerActiveIn() {
        LocalDate date = LocalDate.of(2026, 7, 17);
        given(groupMemberRepository.findActiveGroupIds(USER_ID)).willReturn(List.of(OTHER_GROUP_ID));

        ArchiveDailyResponse response = archiveService.getDailyRecords(USER_ID, GROUP_ID, date);

        assertThat(response.records()).isEmpty();
        verify(archiveEntryRepository, never()).findDailyRecordsInGroups(any(), any(), any());
    }

    @Test
    @DisplayName("일자별 조회는 기록이 없으면 빈 목록을 반환한다")
    void returnsEmptyDailyRecords() {
        LocalDate date = LocalDate.of(2026, 7, 17);
        given(groupMemberRepository.findActiveGroupIds(USER_ID)).willReturn(List.of(GROUP_ID));
        given(archiveEntryRepository.findDailyRecordsInGroups(USER_ID, List.of(GROUP_ID), date))
                .willReturn(List.of());

        ArchiveDailyResponse response = archiveService.getDailyRecords(USER_ID, GROUP_ID, date);

        assertThat(response.records()).isEmpty();
    }

    @Test
    @DisplayName("일자별 통합 조회는 여러 그룹의 카드를 ID 오름차순 스냅샷·썸네일로 반환한다")
    void returnsDailyRecordCardsAcrossGroups() {
        LocalDate date = LocalDate.of(2026, 7, 17);
        given(groupMemberRepository.findActiveGroupIds(USER_ID)).willReturn(List.of(GROUP_ID, OTHER_GROUP_ID));
        ArchiveEntry first = archiveEntry(3L, 10L, date, "첫 번째 질문", "가족", "FAMILY",
                "https://cdn.example.com/thumbnails/10.jpg");
        ArchiveEntry second = archiveEntry(7L, 20L, date, "두 번째 질문", "친구", "FRIEND",
                null);
        // groupId 생략 시 내 ACTIVE 그룹 전체가 그대로 조회 범위로 넘어가야 한다(일부만 넘기면 안 됨).
        given(archiveEntryRepository.findDailyRecordsInGroups(
                USER_ID, List.of(GROUP_ID, OTHER_GROUP_ID), date)).willReturn(List.of(first, second));

        ArchiveDailyResponse response = archiveService.getDailyRecords(USER_ID, null, date);

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
