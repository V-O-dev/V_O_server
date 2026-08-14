package com.example.v_o_server.common.web;

import java.beans.PropertyEditorSupport;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 필드가 파일 파트가 아니라 <b>텍스트</b>로 들어온 경우를 "파일 없음"으로 해석한다.
 *
 * <p>Swagger UI를 비롯한 여러 클라이언트는 파일 선택 칸을 비운 채 전송하면 해당 필드를
 * 빈 문자열 텍스트 파트로 보낸다. 이때 {@code @ModelAttribute} 데이터 바인딩은
 * {@code String → MultipartFile} 변환기를 찾지 못해 다음과 같이 실패한다.</p>
 *
 * <pre>
 * Failed to convert value of type 'java.lang.String' to required type
 * 'org.springframework.web.multipart.MultipartFile' ... no matching editors or conversion strategy found
 * </pre>
 *
 * <p>그 결과 파일이 <b>선택</b>인 API에서도 400이 나서, 이미지 없이 그룹을 만들거나 수정하는
 * 정상 시나리오가 막혔다. 여기서 빈 값을 {@code null}로 바꿔주면 각 서비스가 이미 갖고 있는
 * {@code file != null && !file.isEmpty()} 분기가 그대로 동작한다.</p>
 *
 * <p>실제 파일 파트가 오면 {@code MultipartFile} 값이 그대로 바인딩되어 이 편집기를 타지 않으므로,
 * 정상 업로드 경로의 동작은 바뀌지 않는다.</p>
 */
@ControllerAdvice
public class MultipartFileBinderAdvice {

    @InitBinder
    public void initMultipartFileBinder(WebDataBinder binder) {
        binder.registerCustomEditor(MultipartFile.class, new EmptyTextAsNullMultipartFileEditor());
    }

    /**
     * 텍스트로 들어온 파일 필드를 처리하는 편집기.
     *
     * <p>{@code setAsText}는 입력이 String일 때만 호출된다. 즉 이 편집기가 도는 시점은
     * "파일 칸에 파일이 아닌 값이 왔다"는 뜻이다.</p>
     */
    private static final class EmptyTextAsNullMultipartFileEditor extends PropertyEditorSupport {

        @Override
        public void setAsText(String text) {
            if (text == null || text.isBlank()) {
                // 파일을 고르지 않은 것과 같다.
                setValue(null);
                return;
            }
            // 빈 값이 아닌 문자열은 조용히 무시하지 않는다. 잘못 보낸 요청임을 400으로 알린다.
            throw new IllegalArgumentException("파일 형식이 올바르지 않습니다.");
        }
    }
}
