package com.example.v_o_server.domain.archive.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.v_o_server.common.security.CurrentUserProvider;
import com.example.v_o_server.domain.archive.dto.ArchiveCalendarResponse;
import com.example.v_o_server.domain.archive.dto.ArchiveDailyResponse;
import com.example.v_o_server.domain.archive.service.ArchiveService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ArchiveController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ArchiveController")
class ArchiveControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArchiveService archiveService;
    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    @DisplayName("캘린더 조회는 기록 날짜 목록을 반환한다")
    void getCalendar() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(USER_ID);
        given(archiveService.getCalendar(USER_ID, 100L, 2026, 7))
                .willReturn(new ArchiveCalendarResponse(2026, 7, List.of(1, 15)));

        mockMvc.perform(get("/archives/calendar")
                        .param("groupId", "100").param("year", "2026").param("month", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordDates[0]").value(1))
                .andExpect(jsonPath("$.data.recordDates[1]").value(15));
    }

    @Test
    @DisplayName("월이 12를 넘으면 C001 검증 오류")
    void rejectsInvalidMonth() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(USER_ID);

        mockMvc.perform(get("/archives/calendar")
                        .param("groupId", "100").param("year", "2026").param("month", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("날짜 형식이 잘못되면 C005 타입 오류")
    void rejectsMalformedDate() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(USER_ID);

        mockMvc.perform(get("/archives/daily").param("groupId", "100").param("date", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C005"));
    }

    @Test
    @DisplayName("기록이 없는 날짜는 빈 목록을 반환한다")
    void returnsEmptyDaily() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(USER_ID);
        given(archiveService.getDailyRecords(USER_ID, 100L, LocalDate.of(2026, 7, 17)))
                .willReturn(new ArchiveDailyResponse(List.of()));

        mockMvc.perform(get("/archives/daily").param("groupId", "100").param("date", "2026-07-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }
}
