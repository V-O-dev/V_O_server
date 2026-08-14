package com.example.v_o_server.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 전역 예외 처리기에서 개별 핸들러가 없어 500으로 새는 예외가 없는지 확인한다.
 *
 * <p>{@code MissingServletRequestPartException}은 Spring 기본 동작이 400인데,
 * 핸들러가 없으면 최종 방어선인 {@code @ExceptionHandler(Exception.class)}가 잡아
 * 오히려 500으로 바꿔버린다. 그 회귀를 막는다.</p>
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RequiredPartController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("필수 파트가 누락되면 500이 아니라 400 C008로 응답한다")
    void missingRequestPartReturnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/test/required-part"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("C008"));
    }

    @Test
    @DisplayName("누락된 파트 이름을 메시지에 담는다")
    void missingRequestPartMessageContainsPartName() throws Exception {
        mockMvc.perform(multipart("/test/required-part"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("image")));
    }

    /** 필수 파트를 요구하는 테스트 전용 컨트롤러. */
    @RestController
    static class RequiredPartController {

        @PostMapping(value = "/test/required-part", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        String requiredPart(@RequestPart("image") MultipartFile image) {
            return "ok";
        }
    }
}
