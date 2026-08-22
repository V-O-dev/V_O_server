package com.example.v_o_server.domain.group.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.security.AccountStatusChecker;
import com.example.v_o_server.common.security.JwtAuthenticationFilter;
import com.example.v_o_server.config.SecurityConfig;
import com.example.v_o_server.domain.group.service.GroupInviteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GroupInviteController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("GroupInviteController")
class GroupInviteControllerTest {

    private static final String CODE = "A3F9K2";

    @Autowired
    private MockMvc mockMvc;

    // JwtAuthenticationFilter가 요구하는 협력자. 이 슬라이스에는 domain 구현체가 없어 목으로 채운다.
    @MockitoBean
    private AccountStatusChecker accountStatusChecker;

    @MockitoBean
    private GroupInviteService groupInviteService;

    @Test
    @DisplayName("QR 요청은 PNG 바이트와 캐시 헤더를 반환한다")
    void returnsQrPng() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
        given(groupInviteService.getInviteQrImage(CODE, null)).willReturn(png);

        mockMvc.perform(get("/api/v1/invites/{code}/qr", CODE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(png))
                .andExpect(header().string("Cache-Control", "max-age=3600, private"));
    }

    @Test
    @DisplayName("size 파라미터는 서비스로 그대로 전달된다")
    void passesSizeParameter() throws Exception {
        given(groupInviteService.getInviteQrImage(CODE, 256)).willReturn(new byte[]{1});

        mockMvc.perform(get("/api/v1/invites/{code}/qr", CODE).param("size", "256"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("만료된 코드의 QR 요청은 G007 INVITE_EXPIRED")
    void expiredCodeReturnsError() throws Exception {
        given(groupInviteService.getInviteQrImage(CODE, null))
                .willThrow(new BusinessException(ErrorCode.INVITE_EXPIRED));

        mockMvc.perform(get("/api/v1/invites/{code}/qr", CODE))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("G007"));
    }
}
