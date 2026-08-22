package com.example.v_o_server.domain.group.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.v_o_server.common.jwt.JwtProvider;
import com.example.v_o_server.common.jwt.TokenBlacklistService;
import com.example.v_o_server.common.security.AccountStatusChecker;
import com.example.v_o_server.common.security.JwtAccessDeniedHandler;
import com.example.v_o_server.common.security.JwtAuthenticationEntryPoint;
import com.example.v_o_server.common.security.JwtAuthenticationFilter;
import com.example.v_o_server.config.SecurityConfig;
import com.example.v_o_server.domain.group.service.GroupInviteService;
import com.example.v_o_server.domain.group.service.GroupMemberAliasService;
import com.example.v_o_server.domain.group.service.GroupMemberService;
import com.example.v_o_server.domain.group.service.GroupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 실제 JWT 필터 체인을 활성화해, 인증 없이 Group API를 호출하면 401이 반환되는지 검증한다.
 * (기존 {@link GroupControllerTest}는 필터를 끈 슬라이스라 인증 로직을 검증하지 않는다.)
 */
@WebMvcTest(controllers = GroupController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("GroupController 보안")
class GroupControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // JwtAuthenticationFilter가 요구하는 협력자. 이 슬라이스에는 domain 구현체가 없어 목으로 채운다.
    @MockitoBean
    private AccountStatusChecker accountStatusChecker;

    @MockitoBean
    private GroupService groupService;
    @MockitoBean
    private GroupInviteService groupInviteService;
    @MockitoBean
    private GroupMemberService groupMemberService;
    @MockitoBean
    private GroupMemberAliasService groupMemberAliasService;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;
    /** EntryPoint/AccessDeniedHandler가 주입받는 Jackson2 ObjectMapper. Boot4 슬라이스엔 빈이 없어 mock으로 제공. */
    @MockitoBean
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("토큰 없이 내 그룹 목록을 호출하면 401")
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/groups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 없이 그룹 생성을 호출하면 401")
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupName":"우리 가족","themeCode":"FAMILY",
                                 "notificationStartTime":"20:00:00","notificationEndTime":"21:00:00"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 없이 그룹 멤버 목록을 호출하면 401")
    void memberListRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/groups/100/members"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 없이 멤버 호칭을 설정하면 401")
    void upsertAliasRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/v1/groups/100/members/10/alias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alias\":\"엄마\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 없이 멤버 호칭을 해제하면 401")
    void deleteAliasRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/100/members/10/alias"))
                .andExpect(status().isUnauthorized());
    }
}
