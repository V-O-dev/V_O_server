package com.example.v_o_server.domain.group.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "방장 권한 위임 요청")
public record OwnerTransferRequest(
        @Schema(description = "새 방장이 될 유저 ID", example = "5")
        @NotNull(message = "새 방장 ID는 필수입니다.")
        Long newOwnerId
) {
}
