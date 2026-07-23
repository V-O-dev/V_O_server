package com.example.v_o_server.domain.answer.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.domain.answer.dto.request.CommentContentRequest;
import com.example.v_o_server.domain.answer.dto.response.CommentListResponse;
import com.example.v_o_server.domain.answer.dto.response.CommentResponse;
import com.example.v_o_server.domain.answer.dto.response.CommentUpdateResponse;
import com.example.v_o_server.domain.answer.service.VideoCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Video Comment", description = "영상 댓글 API")
@RestController
@RequiredArgsConstructor
public class VideoCommentController {

    private final VideoCommentService videoCommentService;

    @Operation(summary = "댓글 목록 조회", description = "커서 기반 페이지네이션으로 댓글을 조회합니다. cursor는 마지막으로 받은 commentId.")
    @GetMapping("/api/v1/videos/{videoId}/comments")
    public ApiResponse<CommentListResponse> getComments(@AuthenticationPrincipal Long userId,
            @PathVariable Long videoId,
            @RequestParam(required = false) Long cursor) {
        return ApiResponse.success(videoCommentService.getComments(userId, videoId, cursor));
    }

    @Operation(summary = "댓글 등록", description = "영상에 댓글을 등록합니다. (공백 제외 1자 이상, 최대 100자)")
    @PostMapping("/api/v1/videos/{videoId}/comments")
    public ApiResponse<CommentResponse> createComment(@AuthenticationPrincipal Long userId,
            @PathVariable Long videoId,
            @RequestBody CommentContentRequest request) {
        return ApiResponse.success(videoCommentService.createComment(userId, videoId, request.content()));
    }

    @Operation(summary = "댓글 수정", description = "본인이 작성한 댓글의 내용을 수정합니다.")
    @PatchMapping("/api/v1/comments/{commentId}")
    public ApiResponse<CommentUpdateResponse> updateComment(@AuthenticationPrincipal Long userId,
            @PathVariable Long commentId,
            @RequestBody CommentContentRequest request) {
        return ApiResponse.success(videoCommentService.updateComment(userId, commentId, request.content()));
    }

    @Operation(summary = "댓글 삭제", description = "본인이 작성한 댓글을 삭제합니다. (soft delete)")
    @DeleteMapping("/api/v1/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@AuthenticationPrincipal Long userId,
            @PathVariable Long commentId) {
        videoCommentService.deleteComment(userId, commentId);
        return ApiResponse.ok();
    }
}
