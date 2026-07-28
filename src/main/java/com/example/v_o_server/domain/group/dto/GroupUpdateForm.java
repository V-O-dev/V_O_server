package com.example.v_o_server.domain.group.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * 그룹 수정 multipart 폼. 그룹명·이미지 모두 선택이지만 최소 1개는 있어야 한다(서비스에서 검증).
 *
 * <p>이미지를 폼 객체 안에 두어 Swagger 에 업로드 칸이 함께 문서화되게 한다.</p>
 */
@Schema(description = "그룹 수정 폼")
public record GroupUpdateForm(
        @Schema(description = "변경할 그룹명 (1~15자, 선택)", example = "새 그룹명")
        @Size(min = 1, max = 15, message = "그룹명은 1~15자여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9 ]+$", message = "그룹명에 특수문자는 사용할 수 없습니다.")
        String groupName,

        @Schema(description = "변경할 테마 코드 (선택)", example = "COUPLE")
        String themeCode,

        @Schema(description = "변경할 대표 이미지 (선택)", type = "string", format = "binary")
        MultipartFile image
) {
    public GroupUpdateRequest toRequest() {
        return new GroupUpdateRequest(groupName, themeCode);
    }
}
