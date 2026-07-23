package com.example.v_o_server.domain.answer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.v_o_server.common.jwt.JwtProvider;
import com.example.v_o_server.common.security.JwtAccessDeniedHandler;
import com.example.v_o_server.common.security.JwtAuthenticationEntryPoint;
import com.example.v_o_server.common.security.JwtAuthenticationFilter;
import com.example.v_o_server.common.jwt.TokenBlacklistService;
import com.example.v_o_server.config.SecurityConfig;
import com.example.v_o_server.domain.answer.service.VideoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = VideoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("VideoController 보안")
class VideoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VideoService videoService;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;
    @MockitoBean
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("토큰 없이 영상 업로드를 호출하면 401")
    void uploadRequiresAuthentication() throws Exception {
        MockMultipartFile video = new MockMultipartFile(
                "video",
                "answer.mp4",
                "video/mp4",
                new byte[]{1}
        );

        mockMvc.perform(multipart("/api/v1/videos")
                        .file(video)
                        .param("groupId", "10")
                        .param("questionId", "20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 없이 영상 상세 조회를 호출하면 401")
    void detailRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/videos/100"))
                .andExpect(status().isUnauthorized());
    }
}
