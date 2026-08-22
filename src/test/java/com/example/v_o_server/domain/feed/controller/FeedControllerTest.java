package com.example.v_o_server.domain.feed.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.v_o_server.common.security.AccountStatusChecker;
import com.example.v_o_server.common.security.JwtAuthenticationFilter;
import com.example.v_o_server.config.SecurityConfig;
import com.example.v_o_server.domain.answer.entity.AnswerUploadStatus;
import com.example.v_o_server.domain.feed.dto.FeedItemResponse;
import com.example.v_o_server.domain.feed.dto.FeedResponse;
import com.example.v_o_server.domain.feed.service.FeedService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

@WebMvcTest(controllers = FeedController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FeedController")
class FeedControllerTest {

    private static final Long AUTH_USER_ID = 1L;
    private static final Long GROUP_ID = 100L;
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 7, 24);

    @Autowired
    private MockMvc mockMvc;

    // JwtAuthenticationFilter가 요구하는 협력자. 이 슬라이스에는 domain 구현체가 없어 목으로 채운다.
    @MockitoBean
    private AccountStatusChecker accountStatusChecker;

    @MockitoBean
    private FeedService feedService;
    @MockitoBean
    private Clock clock;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("그룹 피드는 잠금 상태와 영상 목록을 공통 응답으로 반환한다")
    void getFeed() throws Exception {
        FeedItemResponse item = new FeedItemResponse(
                10L,
                2L,
                21L,
                false,
                "동구",
                "막내",
                "막내",
                "https://cdn.example.com/profile.jpg",
                20L,
                "오늘 가장 웃겼던 일은?",
                "https://cdn.example.com/video.mp4",
                "https://cdn.example.com/thumbnail.jpg",
                10_000,
                4,
                true,
                2,
                LocalDateTime.of(2026, 7, 24, 11, 59, 50),
                LocalDateTime.of(2026, 7, 24, 12, 0).atOffset(ZoneOffset.ofHours(9))
        );
        given(feedService.getFeed(AUTH_USER_ID, GROUP_ID, SERVICE_DATE, 0, 20))
                .willReturn(new FeedResponse(
                        true,
                        SERVICE_DATE,
                        AnswerUploadStatus.UPLOADED,
                        List.of(item),
                        0,
                        20,
                        1,
                        1,
                        false
                ));

        mockMvc.perform(get("/api/v1/groups/{groupId}/feed", GROUP_ID)
                        .param("serviceDate", "2026-07-24")
                        .with(authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unlocked").value(true))
                .andExpect(jsonPath("$.data.viewerAnswerStatus").value("UPLOADED"))
                .andExpect(jsonPath("$.data.items[0].videoId").value(10))
                .andExpect(jsonPath("$.data.items[0].nickname").value("동구"))
                .andExpect(jsonPath("$.data.items[0].alias").value("막내"))
                .andExpect(jsonPath("$.data.items[0].displayName").value("막내"))
                .andExpect(jsonPath("$.data.items[0].memberId").value(21))
                // 프론트가 feed.isMe로 읽으므로 직렬화된 키 이름까지 고정한다.
                // Jackson이 boolean 레코드 컴포넌트를 "me"로 깎으면 조용히 깨진다.
                .andExpect(jsonPath("$.data.items[0].isMe").value(false))
                .andExpect(jsonPath("$.data.items[0].questionContent")
                        .value("오늘 가장 웃겼던 일은?"))
                .andExpect(jsonPath("$.data.items[0].reactionCount").value(4))
                .andExpect(jsonPath("$.data.items[0].reactedByMe").value(true))
                .andExpect(jsonPath("$.data.items[0].commentCount").value(2))
                .andExpect(jsonPath("$.data.items[0].uploadedAt")
                        .value("2026-07-24T12:00:00+09:00"))
                .andExpect(jsonPath("$.data.hasNext").value(false));

        verify(feedService).getFeed(AUTH_USER_ID, GROUP_ID, SERVICE_DATE, 0, 20);
    }

    @Test
    @DisplayName("날짜를 생략하면 서버 기본 타임존이 아닌 KST 오늘 날짜를 사용한다")
    void defaultsServiceDateToKoreaDate() throws Exception {
        LocalDate koreaDate = LocalDate.of(2026, 7, 25);
        given(clock.instant()).willReturn(Instant.parse("2026-07-24T15:30:00Z"));
        given(clock.getZone()).willReturn(ZoneId.of("Asia/Seoul"));
        given(feedService.getFeed(AUTH_USER_ID, GROUP_ID, koreaDate, 0, 20))
                .willReturn(new FeedResponse(
                        false,
                        koreaDate,
                        AnswerUploadStatus.NOT_UPLOADED,
                        List.of(),
                        0,
                        20,
                        0,
                        0,
                        false
                ));

        mockMvc.perform(get("/api/v1/groups/{groupId}/feed", GROUP_ID)
                        .with(authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serviceDate").value("2026-07-25"));

        verify(feedService).getFeed(AUTH_USER_ID, GROUP_ID, koreaDate, 0, 20);
    }

    @Test
    @DisplayName("잘못된 날짜 형식은 C005 타입 오류를 반환한다")
    void rejectsMalformedServiceDate() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/feed", GROUP_ID)
                        .param("serviceDate", "not-a-date")
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C005"));
    }

    @Test
    @DisplayName("음수 페이지는 C001 검증 오류를 반환한다")
    void rejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/feed", GROUP_ID)
                        .param("page", "-1")
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("페이지 크기가 50을 넘으면 C001 검증 오류를 반환한다")
    void rejectsOversizedPage() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/feed", GROUP_ID)
                        .param("size", "51")
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
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
