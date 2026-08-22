package com.example.v_o_server.domain.archive.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.v_o_server.common.jwt.JwtProvider;
import com.example.v_o_server.common.jwt.TokenBlacklistService;
import com.example.v_o_server.common.security.AccountStatusChecker;
import com.example.v_o_server.common.security.JwtAccessDeniedHandler;
import com.example.v_o_server.common.security.JwtAuthenticationEntryPoint;
import com.example.v_o_server.common.security.JwtAuthenticationFilter;
import com.example.v_o_server.config.SecurityConfig;
import com.example.v_o_server.domain.archive.service.ArchiveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 실제 JWT 필터 체인을 활성화해, 인증 없이 Archive API를 호출하면 401이 반환되는지 검증한다.
 * (기존 {@link ArchiveControllerTest}는 필터를 끈 슬라이스라 인증 로직을 검증하지 않는다.)
 */
@WebMvcTest(controllers = ArchiveController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("ArchiveController 보안")
class ArchiveControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // JwtAuthenticationFilter가 요구하는 협력자. 이 슬라이스에는 domain 구현체가 없어 목으로 채운다.
    @MockitoBean
    private AccountStatusChecker accountStatusChecker;

    @MockitoBean
    private ArchiveService archiveService;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;
    /** EntryPoint/AccessDeniedHandler가 주입받는 Jackson2 ObjectMapper. Boot4 슬라이스엔 빈이 없어 mock으로 제공. */
    @MockitoBean
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("토큰 없이 캘린더를 호출하면 401")
    void calendarRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/archives/calendar")
                        .param("groupId", "100").param("year", "2026").param("month", "7"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 없이 일자별 조회를 호출하면 401")
    void dailyRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/archives/daily")
                        .param("groupId", "100").param("date", "2026-07-17"))
                .andExpect(status().isUnauthorized());
    }
}
