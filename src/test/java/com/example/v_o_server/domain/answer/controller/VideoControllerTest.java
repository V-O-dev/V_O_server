package com.example.v_o_server.domain.answer.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.security.AccountStatusChecker;
import com.example.v_o_server.common.security.JwtAuthenticationFilter;
import com.example.v_o_server.config.SecurityConfig;
import com.example.v_o_server.domain.answer.dto.request.VideoUploadMetadataRequest;
import com.example.v_o_server.domain.answer.dto.response.VideoResponse;
import com.example.v_o_server.domain.answer.service.VideoService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = VideoController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("VideoController")
class VideoControllerTest {

    private static final Long AUTH_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    // JwtAuthenticationFilter가 요구하는 협력자. 이 슬라이스에는 domain 구현체가 없어 목으로 채운다.
    @MockitoBean
    private AccountStatusChecker accountStatusChecker;

    @MockitoBean
    private VideoService videoService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("multipart 영상과 선택 메타데이터를 업로드 서비스에 전달한다")
    void uploadVideo() throws Exception {
        MockMultipartFile video = new MockMultipartFile(
                "video",
                "answer.mp4",
                "video/mp4",
                new byte[]{1, 2, 3}
        );
        LocalDateTime uploadedAt = LocalDateTime.of(2026, 7, 24, 12, 1);
        given(videoService.uploadVideo(eq(AUTH_USER_ID), any(VideoUploadMetadataRequest.class), eq(video)))
                .willReturn(new VideoResponse(
                        100L,
                        10L,
                        20L,
                        "https://cdn.example.com/video.mp4",
                        null,
                        10_000,
                        uploadedAt
                ));

        mockMvc.perform(multipart("/api/v1/videos")
                        .file(video)
                        .param("groupId", "10")
                        .param("questionId", "20")
                        .param("durationMs", "10000")
                        .param("width", "1080")
                        .param("height", "1920")
                        .param("cameraFacing", "FRONT")
                        .param("capturedAt", "2026-07-24T12:00:00")
                        .with(authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.videoId").value(100))
                .andExpect(jsonPath("$.data.durationMs").value(10_000));

        ArgumentCaptor<VideoUploadMetadataRequest> metadataCaptor =
                ArgumentCaptor.forClass(VideoUploadMetadataRequest.class);
        verify(videoService).uploadVideo(eq(AUTH_USER_ID), metadataCaptor.capture(), eq(video));
        assertThat(metadataCaptor.getValue().groupId()).isEqualTo(10L);
        assertThat(metadataCaptor.getValue().questionId()).isEqualTo(20L);
        assertThat(metadataCaptor.getValue().durationMs()).isEqualTo(10_000);
        assertThat(metadataCaptor.getValue().width()).isEqualTo(1080);
        assertThat(metadataCaptor.getValue().height()).isEqualTo(1920);
        assertThat(metadataCaptor.getValue().cameraFacing()).isEqualTo("FRONT");
        assertThat(metadataCaptor.getValue().capturedAt())
                .isEqualTo(LocalDateTime.of(2026, 7, 24, 12, 0));
    }

    @Test
    @DisplayName("영상 칸이 빈 문자열이면 바인딩을 통과해 서비스 검증(V010)까지 간다")
    void emptyVideoTextReachesServiceValidation() throws Exception {
        willThrow(new BusinessException(ErrorCode.VIDEO_REQUIRED))
                .given(videoService).uploadVideo(eq(AUTH_USER_ID), any(VideoUploadMetadataRequest.class), isNull());

        mockMvc.perform(multipart("/api/v1/videos")
                        .param("groupId", "10")
                        .param("questionId", "20")
                        .param("durationMs", "10000")
                        .param("video", "")
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                // C001(String→MultipartFile 변환 실패)이 아니라 V010이어야 한다
                .andExpect(jsonPath("$.code").value("V010"));
    }

    @Test
    @DisplayName("영상 파트가 아예 없어도 동일하게 V010이다")
    void missingVideoPartReachesServiceValidation() throws Exception {
        willThrow(new BusinessException(ErrorCode.VIDEO_REQUIRED))
                .given(videoService).uploadVideo(eq(AUTH_USER_ID), any(VideoUploadMetadataRequest.class), isNull());

        mockMvc.perform(multipart("/api/v1/videos")
                        .param("groupId", "10")
                        .param("questionId", "20")
                        .param("durationMs", "10000")
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("V010"));
    }

    @Test
    @DisplayName("15초를 넘는 영상 메타데이터는 C001로 거부한다")
    void rejectsTooLongDuration() throws Exception {
        MockMultipartFile video = new MockMultipartFile(
                "video",
                "answer.mp4",
                "video/mp4",
                new byte[]{1}
        );

        mockMvc.perform(multipart("/api/v1/videos")
                        .file(video)
                        .param("groupId", "10")
                        .param("questionId", "20")
                        .param("durationMs", "15001")
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("지원하지 않는 카메라 방향은 C001로 거부한다")
    void rejectsInvalidCameraFacing() throws Exception {
        MockMultipartFile video = new MockMultipartFile(
                "video",
                "answer.mp4",
                "video/mp4",
                new byte[]{1}
        );

        mockMvc.perform(multipart("/api/v1/videos")
                        .file(video)
                        .param("groupId", "10")
                        .param("questionId", "20")
                        .param("cameraFacing", "SIDE")
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("영상 상세 조회는 공통 응답으로 반환한다")
    void getVideo() throws Exception {
        LocalDateTime uploadedAt = LocalDateTime.of(2026, 7, 24, 12, 1);
        given(videoService.getVideo(AUTH_USER_ID, 100L))
                .willReturn(new VideoResponse(
                        100L,
                        10L,
                        20L,
                        "https://cdn.example.com/video.mp4",
                        null,
                        10_000,
                        uploadedAt
                ));

        mockMvc.perform(get("/api/v1/videos/100").with(authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.videoId").value(100))
                .andExpect(jsonPath("$.data.groupId").value(10))
                .andExpect(jsonPath("$.data.questionId").value(20));

        verify(videoService).getVideo(AUTH_USER_ID, 100L);
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
