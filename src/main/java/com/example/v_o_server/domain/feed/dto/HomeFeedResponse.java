package com.example.v_o_server.domain.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "전체 그룹 통합 홈 피드 응답")
public record HomeFeedResponse(
        @Schema(description = "조회 기준 서비스 날짜")
        LocalDate serviceDate,

        @Schema(description = "내가 ACTIVE로 속한 그룹 목록. 오늘 영상이 0건인 그룹도 상단 그룹 탭을 그리도록 포함된다")
        List<HomeFeedGroupResponse> groups,

        @Schema(description = "그룹을 넘나드는 통합 영상 목록. uploadedAt 최신순이며 잠긴 그룹의 영상도 포함된다")
        List<HomeFeedItemResponse> items,

        @Schema(description = "현재 페이지 번호(0부터 시작)")
        int page,

        @Schema(description = "페이지 크기")
        int size,

        @Schema(description = "전체 영상 수")
        long totalElements,

        @Schema(description = "전체 페이지 수")
        int totalPages,

        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext
) {

    public static HomeFeedResponse of(
            LocalDate serviceDate,
            List<HomeFeedGroupResponse> groups,
            Page<HomeFeedItemResponse> items
    ) {
        return new HomeFeedResponse(
                serviceDate,
                groups,
                items.getContent(),
                items.getNumber(),
                items.getSize(),
                items.getTotalElements(),
                items.getTotalPages(),
                items.hasNext()
        );
    }

    /** 내가 ACTIVE로 속한 그룹이 하나도 없을 때. 영상/그룹 조회 쿼리를 아예 태우지 않고 바로 만든다. */
    public static HomeFeedResponse empty(LocalDate serviceDate, int page, int size) {
        return new HomeFeedResponse(serviceDate, List.of(), List.of(), page, size, 0L, 0, false);
    }
}
