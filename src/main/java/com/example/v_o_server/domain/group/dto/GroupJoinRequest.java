package com.example.v_o_server.domain.group.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "초대 코드로 그룹 가입 요청")
public record GroupJoinRequest(
        @Schema(description = "초대 코드", example = "A3F9K2")
        @NotBlank(message = "초대 코드는 필수입니다.")
        String code
) {
}
