package com.example.v_o_server.domain.group.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.security.JwtAuthenticationFilter;
import com.example.v_o_server.config.SecurityConfig;
import com.example.v_o_server.domain.group.dto.GroupNameDuplicateResponse;
import com.example.v_o_server.domain.group.service.GroupInviteService;
import com.example.v_o_server.domain.group.service.GroupMemberService;
import com.example.v_o_server.domain.group.service.GroupService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(controllers = GroupController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("GroupController")
class GroupControllerTest {

    /** 테스트에서 SecurityContext에 심는 인증 사용자 ID. */
    private static final Long AUTH_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

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

    /** 그룹 생성 요청 JSON. groupName만 케이스별로 달라진다. */
    private static String createRequestJson(String groupName) {
        return """
                {
                  "groupName": "%s",
                  "themeCode": "FAMILY",
                  "notificationStartTime": "20:00:00",
                  "notificationEndTime": "21:00:00"
                }
                """.formatted(groupName);
    }

    @MockitoBean
    private GroupService groupService;
    @MockitoBean
    private GroupInviteService groupInviteService;
    @MockitoBean
    private GroupMemberService groupMemberService;

    @Test
    @DisplayName("그룹명 중복 확인은 duplicated 값을 반환한다")
    void checkDuplicate() throws Exception {
        given(groupService.checkNameDuplicated(AUTH_USER_ID, "우리 가족"))
                .willReturn(new GroupNameDuplicateResponse(true));

        mockMvc.perform(get("/api/v1/groups/check-duplicate").param("name", "우리 가족")
                        .with(authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.duplicated").value(true));
    }

    @Test
    @DisplayName("그룹명이 15자를 넘으면 C001 검증 오류")
    void rejectsTooLongName() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("가".repeat(16)))
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("그룹명에 특수문자가 있으면 C001 검증 오류")
    void rejectsSpecialCharacters() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("우리@가족"))
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("DELETE /members/me 는 나가기로, /members/{id} 는 강퇴로 각각 라우팅된다")
    void memberRoutesDoNotCollide() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/100/members/me").with(authUser()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/groups/100/members/11").with(authUser()))
                .andExpect(status().isOk());

        verify(groupMemberService).leaveGroup(AUTH_USER_ID, 100L);
        verify(groupMemberService).kickMember(AUTH_USER_ID, 100L, 11L);
    }

    @Test
    @DisplayName("DELETE /groups/{id} 는 그룹 삭제로 라우팅된다")
    void deleteGroupRoute() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/100").with(authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(groupService).deleteGroup(AUTH_USER_ID, 100L);
    }

    @Test
    @DisplayName("방장이 아닌 사용자의 그룹 삭제는 G004 NOT_GROUP_OWNER")
    void deleteGroupRequiresOwner() throws Exception {
        willThrow(new BusinessException(ErrorCode.NOT_GROUP_OWNER))
                .given(groupService).deleteGroup(AUTH_USER_ID, 100L);

        mockMvc.perform(delete("/api/v1/groups/100").with(authUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("G004"));
    }

    @Test
    @DisplayName("multipart 생성 요청은 이미지와 함께 서비스로 전달된다")
    void createGroupWithImage() throws Exception {
        MockMultipartFile request = new MockMultipartFile("request", null,
                MediaType.APPLICATION_JSON_VALUE, createRequestJson("우리 가족").getBytes(StandardCharsets.UTF_8));
        MockMultipartFile image =
                new MockMultipartFile("image", "a.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/groups").file(request).file(image).with(authUser()))
                .andExpect(status().isOk());

        verify(groupService).createGroup(eq(AUTH_USER_ID), any(), any(MultipartFile.class));
    }

    @Test
    @DisplayName("JSON 생성 요청은 이미지 없이 기존 경로로 동작한다")
    void createGroupWithoutImage() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("우리 가족"))
                        .with(authUser()))
                .andExpect(status().isOk());

        verify(groupService).createGroup(eq(AUTH_USER_ID), any());
    }

    @Test
    @DisplayName("멤버가 아닌 그룹 상세 조회는 G003 NOT_GROUP_MEMBER")
    void detailRequiresMembership() throws Exception {
        given(groupService.getGroupDetail(eq(AUTH_USER_ID), any()))
                .willThrow(new BusinessException(ErrorCode.NOT_GROUP_MEMBER));

        mockMvc.perform(get("/api/v1/groups/100").with(authUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("G003"));
    }
}
