package com.example.v_o_server.domain.archive.service;

import static com.example.v_o_server.domain.group.GroupTestFixtures.group;
import static com.example.v_o_server.domain.group.GroupTestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.archive.dto.ArchiveCalendarResponse;
import com.example.v_o_server.domain.archive.dto.ArchiveDailyResponse;
import com.example.v_o_server.domain.archive.repository.ArchiveEntryRepository;
import com.example.v_o_server.domain.group.service.GroupAccessGuard;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArchiveService")
class ArchiveServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long GROUP_ID = 100L;

    @Mock
    private ArchiveEntryRepository archiveEntryRepository;
    @Mock
    private GroupAccessGuard accessGuard;

    @InjectMocks
    private ArchiveService archiveService;

    @Test
    @DisplayName("캘린더 조회는 해당 월의 내 기록 날짜(일)만 반환한다")
    void returnsRecordDaysOfMonth() {
        given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group(GROUP_ID, user(USER_ID), 15));
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
        given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group(GROUP_ID, user(USER_ID), 15));
        given(archiveEntryRepository.findRecordDates(
                USER_ID, GROUP_ID, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
                .willReturn(List.of());

        ArchiveCalendarResponse response = archiveService.getCalendar(USER_ID, GROUP_ID, 2026, 2);

        assertThat(response.recordDates()).isEmpty();
    }

    @Test
    @DisplayName("그룹 멤버가 아니면 NOT_GROUP_MEMBER")
    void rejectsNonMember() {
        given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group(GROUP_ID, user(USER_ID), 15));
        willThrow(new BusinessException(ErrorCode.NOT_GROUP_MEMBER))
                .given(accessGuard).assertMember(GROUP_ID, USER_ID);

        assertThatThrownBy(() -> archiveService.getCalendar(USER_ID, GROUP_ID, 2026, 7))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_GROUP_MEMBER);
    }

    @Test
    @DisplayName("일자별 조회는 기록이 없으면 빈 목록을 반환한다")
    void returnsEmptyDailyRecords() {
        LocalDate date = LocalDate.of(2026, 7, 17);
        given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group(GROUP_ID, user(USER_ID), 15));
        given(archiveEntryRepository.findByUserIdAndGroupIdAndRecordDate(USER_ID, GROUP_ID, date))
                .willReturn(List.of());

        ArchiveDailyResponse response = archiveService.getDailyRecords(USER_ID, GROUP_ID, date);

        assertThat(response.records()).isEmpty();
    }
}
