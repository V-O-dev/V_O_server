package com.example.v_o_server.domain.question.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.v_o_server.common.jwt.JwtProvider;
import com.example.v_o_server.common.jwt.TokenBlacklistService;
import com.example.v_o_server.common.security.AccountStatusChecker;
import com.example.v_o_server.common.security.JwtAccessDeniedHandler;
import com.example.v_o_server.common.security.JwtAuthenticationEntryPoint;
import com.example.v_o_server.common.security.JwtAuthenticationFilter;
import com.example.v_o_server.config.SecurityConfig;
import com.example.v_o_server.domain.question.service.QuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = QuestionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("QuestionController 보안")
class QuestionControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // JwtAuthenticationFilter가 요구하는 협력자. 이 슬라이스에는 domain 구현체가 없어 목으로 채운다.
    @MockitoBean
    private AccountStatusChecker accountStatusChecker;

    @MockitoBean
    private QuestionService questionService;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;
    @MockitoBean
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("토큰 없이 오늘의 질문을 호출하면 401")
    void dailyQuestionRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/questions/daily")
                        .param("groupId", "100"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 없이 답변 대기 중 질문 목록을 호출하면 401")
    void unansweredQuestionsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/questions/unanswered"))
                .andExpect(status().isUnauthorized());
    }
}
