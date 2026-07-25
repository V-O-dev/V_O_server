package com.example.v_o_server.domain.archive.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.v_o_server.common.security.JwtAuthenticationFilter;
import com.example.v_o_server.config.SecurityConfig;
import com.example.v_o_server.domain.archive.dto.ArchiveCalendarResponse;
import com.example.v_o_server.domain.archive.dto.ArchiveDailyResponse;
import com.example.v_o_server.domain.archive.dto.ArchiveRecordResponse;
import com.example.v_o_server.domain.archive.service.ArchiveService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = ArchiveController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ArchiveController")
class ArchiveControllerTest {

    /** 테스트에서 SecurityContext에 심는 인증 사용자 ID. */
    private static final Long AUTH_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArchiveService archiveService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * JWT 필터가 심는 것과 동일하게 principal=userId 인 인증을 SecurityContext에 주입한다.
     * (addFilters=false 슬라이스라 필터가 없으므로 직접 설정하고, {@link #clearSecurityContext()}로 정리한다.)
     */
    private static RequestPostProcessor authUser() {
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(AUTH_USER_ID, null, List.of()));
            SecurityContextHolder.setContext(context);
            return request;
        };
    }

    @Test
    @DisplayName("캘린더 조회는 기록 날짜 목록을 반환한다")
    void getCalendar() throws Exception {
        given(archiveService.getCalendar(AUTH_USER_ID, 100L, 2026, 7))
                .willReturn(new ArchiveCalendarResponse(2026, 7, List.of(1, 15)));

        mockMvc.perform(get("/api/v1/archives/calendar")
                        .param("groupId", "100").param("year", "2026").param("month", "7")
                        .with(authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordDates[0]").value(1))
                .andExpect(jsonPath("$.data.recordDates[1]").value(15));
    }

    @Test
    @DisplayName("월이 12를 넘으면 C001 검증 오류")
    void rejectsInvalidMonth() throws Exception {
        mockMvc.perform(get("/api/v1/archives/calendar")
                        .param("groupId", "100").param("year", "2026").param("month", "13")
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("날짜 형식이 잘못되면 C005 타입 오류")
    void rejectsMalformedDate() throws Exception {
        mockMvc.perform(get("/api/v1/archives/daily").param("groupId", "100").param("date", "not-a-date")
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C005"));
    }

    @Test
    @DisplayName("기록이 없는 날짜는 빈 목록을 반환한다")
    void returnsEmptyDaily() throws Exception {
        given(archiveService.getDailyRecords(AUTH_USER_ID, 100L, LocalDate.of(2026, 7, 17)))
                .willReturn(new ArchiveDailyResponse(List.of()));

        mockMvc.perform(get("/api/v1/archives/daily").param("groupId", "100").param("date", "2026-07-17")
                        .with(authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    @Test
    @DisplayName("일자별 기록은 카드 스냅샷과 영상 썸네일을 반환한다")
    void returnsDailyRecordWithThumbnail() throws Exception {
        LocalDate date = LocalDate.of(2026, 7, 17);
        ArchiveRecordResponse record = new ArchiveRecordResponse(
                1L, date, "오늘 가장 기뻤던 순간은?", "우리 가족", "FAMILY", 10L,
                "https://cdn.example.com/thumbnails/10.jpg");
        given(archiveService.getDailyRecords(AUTH_USER_ID, 100L, date))
                .willReturn(new ArchiveDailyResponse(List.of(record)));

        mockMvc.perform(get("/api/v1/archives/daily")
                        .param("groupId", "100").param("date", "2026-07-17")
                        .with(authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].archiveId").value(1))
                .andExpect(jsonPath("$.data.records[0].questionContent")
                        .value("오늘 가장 기뻤던 순간은?"))
                .andExpect(jsonPath("$.data.records[0].groupName").value("우리 가족"))
                .andExpect(jsonPath("$.data.records[0].groupTheme").value("FAMILY"))
                .andExpect(jsonPath("$.data.records[0].videoId").value(10))
                .andExpect(jsonPath("$.data.records[0].thumbnailUrl")
                        .value("https://cdn.example.com/thumbnails/10.jpg"));
    }

    @Test
    @DisplayName("캘린더 조회에서 groupId를 생략하면 서비스에 null(통합 조회)이 전달된다")
    void getCalendarWithoutGroupId() throws Exception {
        given(archiveService.getCalendar(AUTH_USER_ID, null, 2026, 7))
                .willReturn(new ArchiveCalendarResponse(2026, 7, List.of(3)));

        mockMvc.perform(get("/api/v1/archives/calendar")
                        .param("year", "2026").param("month", "7")
                        .with(authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordDates[0]").value(3));
    }

    @Test
    @DisplayName("일자별 조회에서 groupId를 생략하면 서비스에 null(통합 조회)이 전달된다")
    void getDailyWithoutGroupId() throws Exception {
        given(archiveService.getDailyRecords(AUTH_USER_ID, null, LocalDate.of(2026, 7, 17)))
                .willReturn(new ArchiveDailyResponse(List.of()));

        mockMvc.perform(get("/api/v1/archives/daily").param("date", "2026-07-17")
                        .with(authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }
}
