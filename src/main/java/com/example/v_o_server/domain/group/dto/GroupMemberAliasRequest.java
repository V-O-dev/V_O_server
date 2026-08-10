package com.example.v_o_server.domain.group.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 멤버 호칭 설정 요청.
 *
 * <p><b>공백은 오류가 아니라 "호칭 해제"다.</b> 화면 정의서 GRP_MNG_03의 유효성 규칙
 * "공백 상태로 저장 시 원래 이름으로 롤백 처리"에 맞춘 것으로, 입력 필드를 전체 삭제(X) 버튼으로
 * 비운 뒤 저장하면 호칭이 지워지고 상대의 원래 이름으로 돌아간다. 그래서 {@code @NotBlank}를 걸지 않는다.</p>
 *
 * <p>반대로 앞뒤에 공백이 섞인 값("{@code  엄마 }")은 <b>거부</b>한다. 전역 닉네임
 * ({@code UpdateNicknameRequest})과 같은 규칙을 쓰기 위해서다 — 두 입력의 허용 문자가 갈리면
 * 같은 이름을 프로필에서는 못 쓰고 호칭에서는 쓸 수 있는 식으로 어긋난다.</p>
 */
@Schema(description = "멤버 호칭 설정 요청")
public record GroupMemberAliasRequest(

        @Schema(description = "이 그룹에서 나에게만 보일 호칭. 빈 값이나 공백만 보내면 호칭이 해제되어 원래 이름으로 돌아갑니다.",
                example = "엄마", nullable = true)
        @Size(max = 15, message = "호칭은 최대 15자까지 입력 가능합니다.")
        // 공백만 있는 값(해제 의도) 또는 한글·영문·숫자 조합만 허용한다.
        @Pattern(regexp = "^\\s*$|^[가-힣a-zA-Z0-9]+$", message = "특수문자 및 이모지는 포함할 수 없습니다.")
        String alias
) {
}
