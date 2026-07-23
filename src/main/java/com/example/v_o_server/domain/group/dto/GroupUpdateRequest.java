package com.example.v_o_server.domain.group.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 그룹 정보 수정 요청 (multipart). 두 필드 모두 선택이지만 최소 1개는 있어야 한다.
 * 이미지는 multipart 파트로 별도 전달된다.
 */
@Schema(description = "그룹 정보 수정 요청")
public record GroupUpdateRequest(
        @Schema(description = "변경할 그룹명 (1~15자)", example = "새 그룹명")
        @Size(min = 1, max = 15, message = "그룹명은 1~15자여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9 ]+$", message = "그룹명에 특수문자는 사용할 수 없습니다.")
        String groupName
) {
}
