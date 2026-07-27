package com.example.v_o_server.domain.group.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

/**
 * 그룹 생성(이미지 포함) multipart 폼.
 *
 * <p>{@link GroupCreateRequest} 와 같은 필드에 이미지 파일을 더한 폼 바인딩 전용 DTO.
 * 이미지를 {@code @RequestPart} 로 분리하면 springdoc 이 스키마에서 파일 필드를 누락시켜
 * Swagger 에 업로드 칸이 안 뜨므로, 파일을 폼 객체 안에 두어 함께 문서화되게 한다.</p>
 */
@Schema(description = "그룹 생성(이미지 포함) 폼")
public record GroupCreateForm(
        @Schema(description = "그룹명 (1~15자, 한글/영문/숫자/공백)", example = "우리 가족")
        @NotBlank(message = "그룹명은 필수입니다.")
        @Size(min = 1, max = 15, message = "그룹명은 1~15자여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9 ]+$", message = "그룹명에 특수문자는 사용할 수 없습니다.")
        String groupName,

        @Schema(description = "테마 코드", example = "FAMILY")
        @NotBlank(message = "테마 코드는 필수입니다.")
        String themeCode,

        @Schema(description = "질문 알림 시작 시간", example = "20:00")
        @NotNull(message = "알림 시작 시간은 필수입니다.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
        LocalTime notificationStartTime,

        @Schema(description = "질문 알림 종료 시간", example = "21:00")
        @NotNull(message = "알림 종료 시간은 필수입니다.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
        LocalTime notificationEndTime,

        @Schema(description = "대표 이미지 (선택)", type = "string", format = "binary")
        MultipartFile image
) {
    public GroupCreateRequest toRequest() {
        return new GroupCreateRequest(groupName, themeCode, notificationStartTime, notificationEndTime);
    }
}
