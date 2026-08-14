package com.example.v_o_server.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.security.AccountStatusChecker;
import com.example.v_o_server.common.security.JwtAuthenticationFilter;
import com.example.v_o_server.config.SecurityConfig;
import com.example.v_o_server.domain.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 프로필 이미지 업로드의 multipart 입력 처리를 검증한다.
 *
 * <p>이 API는 이미지가 <b>필수</b>이므로 {@code @RequestPart}를 {@code required = true}로 둔다.
 * 관심사는 잘못된 입력이 <b>어떤 경우에도 500이 되지 않는 것</b>이다.</p>
 *
 * <p>파트 없음 / 빈 텍스트 / 0바이트 파일 — 사용자에게는 모두 "이미지를 안 보냈다"는 같은 상황이므로
 * <b>셋 다 400 {@code U008}</b>로 통일한다. 수정 전에는 앞의 두 경우가 500이었다.</p>
 */
@WebMvcTest(controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController")
class UserControllerTest {

    private static final Long AUTH_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    // JwtAuthenticationFilter가 요구하는 협력자. 이 슬라이스에는 domain 구현체가 없어 목으로 채운다.
    @MockitoBean
    private AccountStatusChecker accountStatusChecker;

    @MockitoBean
    private UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static RequestPostProcessor authUser() {
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(AUTH_USER_ID, null, List.of()));
            SecurityContextHolder.setContext(context);
            return request;
        };
    }

    /** 서비스는 이미지가 없거나 비어 있으면 U008을 던진다. 컨트롤러가 거기까지 도달하는지가 이 테스트의 관심사다. */
    private void givenServiceRejectsEmptyImage() {
        willThrow(new BusinessException(ErrorCode.IMAGE_REQUIRED))
                .given(userService).updateProfileImage(anyLong(), any());
    }

    @Test
    @DisplayName("이미지 파트가 없으면 500이 아니라 400 U008로 응답한다")
    void updateProfileImageWithoutPart() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/users/me/profile/image")
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("U008"));
    }

    @Test
    @DisplayName("이미지 칸이 빈 문자열이어도 500이 아니라 400 U008로 응답한다")
    void updateProfileImageWithEmptyText() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/users/me/profile/image")
                        .param("image", "")
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("U008"));
    }

    @Test
    @DisplayName("0바이트 파일 파트여도 400 U008로 응답한다")
    void updateProfileImageWithEmptyFile() throws Exception {
        givenServiceRejectsEmptyImage();

        MockMultipartFile empty =
                new MockMultipartFile("image", "", MediaType.IMAGE_PNG_VALUE, new byte[0]);

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/users/me/profile/image")
                        .file(empty)
                        .with(authUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("U008"));
    }

    @Test
    @DisplayName("정상 이미지는 서비스로 전달된다")
    void updateProfileImageWithRealFile() throws Exception {
        given(userService.updateProfileImage(anyLong(), any()))
                .willReturn(null);

        MockMultipartFile image =
                new MockMultipartFile("image", "a.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/users/me/profile/image")
                        .file(image)
                        .with(authUser()))
                .andExpect(status().isOk());
    }
}
