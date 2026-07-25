package com.example.v_o_server.domain.answer.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.domain.answer.dto.response.ReactionResponse;
import com.example.v_o_server.domain.answer.service.VideoReactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Video Reaction", description = "영상 좋아요 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/videos/{videoId}/reactions")
public class VideoReactionController {

    private final VideoReactionService videoReactionService;

    @Operation(summary = "좋아요 추가", description = "영상에 좋아요를 추가합니다. 이미 누른 상태면 멱등 처리됩니다.")
    @PostMapping
    public ApiResponse<ReactionResponse> addReaction(@AuthenticationPrincipal Long userId,
            @PathVariable Long videoId) {
        return ApiResponse.success(videoReactionService.addReaction(userId, videoId));
    }

    @Operation(summary = "좋아요 취소", description = "영상에 눌렀던 좋아요를 취소합니다.")
    @DeleteMapping
    public ApiResponse<ReactionResponse> cancelReaction(@AuthenticationPrincipal Long userId,
            @PathVariable Long videoId) {
        return ApiResponse.success(videoReactionService.cancelReaction(userId, videoId));
    }
}
