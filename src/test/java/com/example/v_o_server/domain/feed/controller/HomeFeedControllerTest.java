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
import com.example.v_o_server.domain.feed.dto.HomeFeedGroupResponse;
import com.example.v_o_server.domain.feed.dto.HomeFeedItemResponse;
import com.example.v_o_server.domain.feed.dto.HomeFeedResponse;
import com.example.v_o_server.domain.feed.service.HomeFeedService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

@WebMvcTest(controllers = HomeFeedController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("HomeFeedController")
class HomeFeedControllerTest {

    private static final Long AUTH_USER_ID = 1L;
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 8, 12);

    @Autowired
    private MockMvc mockMvc;

    // JwtAuthenticationFilter가 요구하는 협력자. 이 슬라이스에는 domain 구현체가 없어 목으로 채운다.
    @MockitoBean
    private AccountStatusChecker accountStatusChecker;

    @MockitoBean
    private HomeFeedService homeFeedService;
    @MockitoBean
    private Clock clock;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("홈 피드는 그룹 목록과 통합 영상 목록을 함께 반환한다")
    void getHomeFeed() throws Exception {
        FeedItemResponse video = new FeedItemResponse(
                105L,
                3L,
                21L,
                false,
                "김동생",
                "동생",
                "동생",
                "https://cdn.example.com/profile.jpg",
                20L,
                "오늘 가장 웃겼던 일은?",
                "https://cdn.example.com/video.mp4",
                null,
                10_000,
                3,
                true,
                1,
                LocalDateTime.of(2026, 8, 12, 11, 59, 50),
                LocalDateTime.of(2026, 8, 12, 12, 0)
        );
        HomeFeedResponse response = HomeFeedResponse.of(
                SERVICE_DATE,
                List.of(new HomeFeedGroupResponse(10L, "우리 가족", "https://cdn.example.com/g.jpg",
                        true, AnswerUploadStatus.UPLOADED)),
                new org.springframework.data.domain.PageImpl<>(
                        List.of(new HomeFeedItemResponse(10L, "우리 가족", true, video)),
                        org.springframework.data.domain.PageRequest.of(0, 20),
                        1
                )
        );
        given(homeFeedService.getHomeFeed(AUTH_USER_ID, SERVICE_DATE, 0, 20)).willReturn(response);

        mockMvc.perform(get("/api/v1/feeds/home")
                        .param("serviceDate", "2026-08-12")
                        .with(authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceDate").value("2026-08-12"))
                .andExpect(jsonPath("$.data.groups[0].groupId").value(10))
                // GET /api/v1/groups(GroupSummaryResponse)와 같은 필드명(name/imageUrl)으로 내려가야
                // 클라이언트가 그룹 요약 정보를 한 형태로 다룰 수 있다 — groupName/groupImageUrl로 새면 조용히 깨진다.
                .andExpect(jsonPath("$.data.groups[0].name").value("우리 가족"))
                .andExpect(jsonPath("$.data.groups[0].imageUrl").value("https://cdn.example.com/g.jpg"))
                .andExpect(jsonPath("$.data.groups[0].unlocked").value(true))
                .andExpect(jsonPath("$.data.items[0].groupId").value(10))
                .andExpect(jsonPath("$.data.items[0].groupName").value("우리 가족"))
                .andExpect(jsonPath("$.data.items[0].unlocked").value(true))
                .andExpect(jsonPath("$.data.items[0].video.videoId").value(105))
                .andExpect(jsonPath("$.data.items[0].video.displayName").value("동생"))
                .andExpect(jsonPath("$.data.items[0].video.isMe").value(false))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(homeFeedService).getHomeFeed(AUTH_USER_ID, SERVICE_DATE, 0, 20);
    }

    @Test
    @DisplayName("날짜를 생략하면 서버 기본 타임존이 아닌 KST 오늘 날짜를 사용한다")
    void defaultsServiceDateToKoreaDate() throws Exception {
        LocalDate koreaDate = LocalDate.of(2026, 7, 25);
        given(clock.instant()).willReturn(Instant.parse("2026-07-24T15:30:00Z"));
        given(clock.getZone()).willReturn(ZoneId.of("Asia/Seoul"));
        given(homeFeedService.getHomeFeed(AUTH_USER_ID, koreaDate, 0, 20))
                .willReturn(HomeFeedResponse.empty(koreaDate, 0, 20));

        mockMvc.perform(get("/api/v1/feeds/home").with(authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serviceDate").value("2026-07-25"));

        verify(homeFeedService).getHomeFeed(AUTH_USER_ID, koreaDate, 0, 20);
    }

    @Test
    @DisplayName("잘못된 날짜 형식은 C005 타입 오류를 반환한다")
    void rejectsMalformedServiceDate() throws Exception {
        mockMvc.perform(get("/api/v1/feeds/home")
                        .param("serviceDate", "not-a-date")
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C005"));
    }

    @Test
    @DisplayName("음수 페이지는 C001 검증 오류를 반환한다")
    void rejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/v1/feeds/home")
                        .param("page", "-1")
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("페이지 크기가 50을 넘으면 C001 검증 오류를 반환한다")
    void rejectsOversizedPage() throws Exception {
        mockMvc.perform(get("/api/v1/feeds/home")
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
