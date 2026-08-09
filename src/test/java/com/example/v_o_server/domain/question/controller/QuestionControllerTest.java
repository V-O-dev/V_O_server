package com.example.v_o_server.domain.question.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.security.JwtAuthenticationFilter;
import com.example.v_o_server.config.SecurityConfig;
import com.example.v_o_server.domain.question.dto.response.DailyQuestionResponse;
import com.example.v_o_server.domain.question.dto.response.UnansweredQuestionResponse;
import com.example.v_o_server.domain.question.service.QuestionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

@WebMvcTest(controllers = QuestionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("QuestionController")
class QuestionControllerTest {

    private static final Long AUTH_USER_ID = 1L;
    private static final Long GROUP_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionService questionService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 사용자와 그룹 ID를 서비스에 전달해 오늘의 질문을 조회한다")
    void getDailyQuestion() throws Exception {
        DailyQuestionResponse response = new DailyQuestionResponse(
                10L,
                20L,
                "오늘 가장 고마웠던 일은?",
                10_000,
                LocalDate.of(2026, 7, 28),
                LocalDateTime.of(2026, 7, 29, 0, 0)
        );
        given(questionService.getDailyQuestion(AUTH_USER_ID, GROUP_ID))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/questions/daily")
                        .param("groupId", GROUP_ID.toString())
                        .with(authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.groupDailyQuestionId").value(10))
                .andExpect(jsonPath("$.data.questionId").value(20))
                .andExpect(jsonPath("$.data.content").value("오늘 가장 고마웠던 일은?"))
                .andExpect(jsonPath("$.data.answerTimeLimitMs").value(10000));

        verify(questionService).getDailyQuestion(AUTH_USER_ID, GROUP_ID);
    }

    @Test
    @DisplayName("그룹 비멤버는 G003과 403을 반환한다")
    void rejectsNonMember() throws Exception {
        given(questionService.getDailyQuestion(AUTH_USER_ID, GROUP_ID))
                .willThrow(new BusinessException(ErrorCode.NOT_GROUP_MEMBER));

        mockMvc.perform(get("/api/v1/questions/daily")
                        .param("groupId", GROUP_ID.toString())
                        .with(authUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("G003"));
    }

    @Test
    @DisplayName("인증 사용자 기준으로 답변 대기 중인 질문 목록을 조회한다")
    void getUnansweredQuestions() throws Exception {
        UnansweredQuestionResponse response = new UnansweredQuestionResponse(
                1L,
                "우리 가족",
                10L,
                20L,
                "오늘 가장 고마웠던 일은?",
                10_000,
                LocalDate.of(2026, 7, 28),
                LocalDateTime.of(2026, 7, 29, 0, 0)
        );
        given(questionService.getUnansweredQuestions(AUTH_USER_ID))
                .willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/questions/unanswered").with(authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].groupId").value(1))
                .andExpect(jsonPath("$.data[0].groupName").value("우리 가족"))
                .andExpect(jsonPath("$.data[0].questionId").value(20));

        verify(questionService).getUnansweredQuestions(AUTH_USER_ID);
    }

    private static RequestPostProcessor authUser() {
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(AUTH_USER_ID, null, List.of()));
            SecurityContextHolder.setContext(context);
            return request;
        };
    }
}
