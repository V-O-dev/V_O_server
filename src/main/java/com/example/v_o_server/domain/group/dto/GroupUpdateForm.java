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
        /*
         * 빈 문자열은 오류가 아니라 "그룹명은 수정하지 않음"이다.
         * Bean Validation의 @Size·@Pattern은 null은 통과시키지만 ""는 검사하는데,
         * 폼 클라이언트는 입력하지 않은 텍스트 칸도 ""로 보낸다. 그래서 min=1과 '+' 패턴을 그대로 두면
         * 테마나 이미지만 바꾸는 정상 요청이 400으로 막힌다.
         * 빈 값 판단은 GroupService#updateGroup의 isBlank() 분기에 위임한다.
         * (같은 문제를 GroupMemberAliasRequest가 먼저 이 방식으로 해결했다.)
         */
        @Schema(description = "변경할 그룹명 (1~15자, 선택). 비워 보내면 그룹명은 변경하지 않습니다.", example = "새 그룹명")
        @Size(max = 15, message = "그룹명은 1~15자여야 합니다.")
        @Pattern(regexp = "^$|^[가-힣a-zA-Z0-9 ]+$", message = "그룹명에 특수문자는 사용할 수 없습니다.")
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
