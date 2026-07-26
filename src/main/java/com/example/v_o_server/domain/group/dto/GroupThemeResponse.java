package com.example.v_o_server.domain.group.dto;

import com.example.v_o_server.domain.group.entity.GroupTheme;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "그룹 테마 응답")
public record GroupThemeResponse(
        @Schema(description = "테마 코드", example = "FRIEND")
        String code,

        @Schema(description = "테마 이름", example = "친구")
        String name,

        @Schema(description = "테마 설명", example = "친구들과 함께하는 그룹")
        String description
) {
    public static GroupThemeResponse from(GroupTheme theme) {
        return new GroupThemeResponse(theme.getCode(), theme.getName(), theme.getDescription());
    }
}
