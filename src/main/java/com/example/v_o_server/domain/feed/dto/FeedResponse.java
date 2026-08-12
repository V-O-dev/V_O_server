package com.example.v_o_server.domain.feed.dto;

import com.example.v_o_server.domain.answer.entity.AnswerUploadStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "그룹 영상 피드 응답")
public record FeedResponse(
        @Schema(description = "피드 잠금 해제 여부. false이면 클라이언트에서 영상 피드를 블러 처리")
        boolean unlocked,

        @Schema(description = "조회 기준 서비스 날짜")
        LocalDate serviceDate,

        @Schema(description = "조회자의 해당 날짜 답변 상태")
        AnswerUploadStatus viewerAnswerStatus,

        @Schema(description = "피드 영상 목록. 잠금 상태에서도 블러 표시를 위해 다른 멤버의 영상이 포함됨")
        List<FeedItemResponse> items,

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

    public static FeedResponse locked(
            LocalDate serviceDate,
            AnswerUploadStatus viewerAnswerStatus,
            Page<FeedItemResponse> items
    ) {
        return from(false, serviceDate, viewerAnswerStatus, items);
    }

    public static FeedResponse unlocked(
            LocalDate serviceDate,
            AnswerUploadStatus viewerAnswerStatus,
            Page<FeedItemResponse> items
    ) {
        return from(true, serviceDate, viewerAnswerStatus, items);
    }

    private static FeedResponse from(
            boolean unlocked,
            LocalDate serviceDate,
            AnswerUploadStatus viewerAnswerStatus,
            Page<FeedItemResponse> items
    ) {
        return new FeedResponse(
                unlocked,
                serviceDate,
                viewerAnswerStatus,
                items.getContent(),
                items.getNumber(),
                items.getSize(),
                items.getTotalElements(),
                items.getTotalPages(),
                items.hasNext()
        );
    }
}
