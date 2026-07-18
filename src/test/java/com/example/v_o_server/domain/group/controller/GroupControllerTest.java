package com.example.v_o_server.domain.group.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.group.dto.GroupNameDuplicateResponse;
import com.example.v_o_server.domain.group.service.GroupInviteService;
import com.example.v_o_server.domain.group.service.GroupMemberService;
import com.example.v_o_server.domain.group.service.GroupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GroupController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("GroupController")
class GroupControllerTest {

    /** 인증 도입 전까지 컨트롤러가 사용하는 고정 사용자 ID (data.sql seed 기준). */
    private static final Long TEMP_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

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
        given(groupService.checkNameDuplicated(TEMP_USER_ID, "우리 가족"))
                .willReturn(new GroupNameDuplicateResponse(true));

        mockMvc.perform(get("/api/v1/groups/check-duplicate").param("name", "우리 가족"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.duplicated").value(true));
    }

    @Test
    @DisplayName("그룹명이 15자를 넘으면 C001 검증 오류")
    void rejectsTooLongName() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("가".repeat(16))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("그룹명에 특수문자가 있으면 C001 검증 오류")
    void rejectsSpecialCharacters() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("우리@가족")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("DELETE /members/me 는 나가기로, /members/{id} 는 강퇴로 각각 라우팅된다")
    void memberRoutesDoNotCollide() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/100/members/me"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/groups/100/members/11"))
                .andExpect(status().isOk());

        verify(groupMemberService).leaveGroup(TEMP_USER_ID, 100L);
        verify(groupMemberService).kickMember(TEMP_USER_ID, 100L, 11L);
    }

    @Test
    @DisplayName("멤버가 아닌 그룹 상세 조회는 G003 NOT_GROUP_MEMBER")
    void detailRequiresMembership() throws Exception {
        given(groupService.getGroupDetail(eq(TEMP_USER_ID), any()))
                .willThrow(new BusinessException(ErrorCode.NOT_GROUP_MEMBER));

        mockMvc.perform(get("/api/v1/groups/100"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("G003"));
    }
}
