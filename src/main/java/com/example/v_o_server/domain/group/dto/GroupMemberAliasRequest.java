package com.example.v_o_server.domain.group.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "멤버 호칭 설정 요청")
public record GroupMemberAliasRequest(

        @Schema(description = "이 그룹에서 나에게만 보일 호칭", example = "엄마")
        @NotBlank(message = "공백을 제외한 한 글자 이상의 문자를 입력해주세요.")
        @Size(max = 15, message = "호칭은 최대 15자까지 입력 가능합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]*$", message = "특수문자 및 이모지는 포함할 수 없습니다.")
        String alias
) {
}
