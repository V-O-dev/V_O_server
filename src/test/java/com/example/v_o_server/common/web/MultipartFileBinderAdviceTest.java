package com.example.v_o_server.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.v_o_server.common.exception.GlobalExceptionHandler;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 필드가 파일 파트가 아니라 텍스트로 들어왔을 때의 바인딩 동작을 검증한다.
 *
 * <p>Swagger UI는 파일 선택 칸을 비운 채 전송하면 해당 필드를 빈 문자열 텍스트 파트로 보낸다.
 * 이 경우 기본 바인딩은 String → MultipartFile 변환기를 찾지 못해 실패하므로,
 * {@link MultipartFileBinderAdvice}가 "선택 안 함"으로 해석해 주어야 한다.</p>
 */
@DisplayName("MultipartFileBinderAdvice")
class MultipartFileBinderAdviceTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestUploadController())
                .setControllerAdvice(new MultipartFileBinderAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("파일 칸이 빈 문자열로 오면 null로 바인딩한다")
    void bindsEmptyTextAsNull() throws Exception {
        mockMvc.perform(multipart("/test/upload").param("file", ""))
                .andExpect(status().isOk())
                .andExpect(content().string("none"));
    }

    @Test
    @DisplayName("파일 칸이 공백 문자열로 와도 null로 바인딩한다")
    void bindsBlankTextAsNull() throws Exception {
        mockMvc.perform(multipart("/test/upload").param("file", "   "))
                .andExpect(status().isOk())
                .andExpect(content().string("none"));
    }

    @Test
    @DisplayName("파일 파트가 아예 없으면 null로 바인딩한다")
    void bindsMissingPartAsNull() throws Exception {
        mockMvc.perform(multipart("/test/upload"))
                .andExpect(status().isOk())
                .andExpect(content().string("none"));
    }

    @Test
    @DisplayName("정상 파일 파트는 그대로 바인딩한다")
    void bindsRealFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/test/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("a.png"));
    }

    @Test
    @DisplayName("파일 칸에 빈 값이 아닌 문자열이 오면 400으로 거부한다")
    void rejectsNonBlankText() throws Exception {
        mockMvc.perform(multipart("/test/upload").param("file", "not-a-file"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors[0].field").value("file"))
                // 편집기가 던진 메시지가 실제로 응답에 실리는지까지 확인한다.
                // 이 단언이 없으면 편집기를 떼어내도(변환 자체가 실패해 같은 C001) 테스트가 통과해버린다.
                .andExpect(jsonPath("$.errors[0].reason")
                        .value(org.hamcrest.Matchers.containsString("파일 형식이 올바르지 않습니다")));
    }

    /** 운영 컨트롤러와 같은 형태(@Valid + @ModelAttribute + MultipartFile 필드)의 테스트 전용 컨트롤러. */
    @RestController
    static class TestUploadController {

        @PostMapping(value = "/test/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        String upload(@Valid @ModelAttribute TestForm form) {
            return form.file() == null ? "none" : form.file().getOriginalFilename();
        }
    }

    record TestForm(MultipartFile file) {
    }
}
