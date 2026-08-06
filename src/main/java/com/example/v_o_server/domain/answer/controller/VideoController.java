package com.example.v_o_server.domain.answer.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.domain.answer.dto.request.VideoUploadForm;
import com.example.v_o_server.domain.answer.dto.response.VideoResponse;
import com.example.v_o_server.domain.answer.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Video", description = "답변 영상 관련 API")
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @Operation(summary = "영상 업로드", description = "오늘의 질문에 대한 답변 영상을 업로드합니다.")
    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<VideoResponse> uploadVideo(
            @AuthenticationPrincipal Long userId,
            @Valid @ModelAttribute VideoUploadForm form) {
        VideoResponse response = videoService.uploadVideo(userId, form.toMetadata(), form.video());
        return ApiResponse.success(response);
    }

    @Operation(summary = "영상 상세 조회", description = "답변 영상 상세 정보를 조회합니다.")
    @GetMapping("/{videoId}")
    public ApiResponse<VideoResponse> getVideo(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long videoId) {
        VideoResponse response = videoService.getVideo(userId, videoId);
        return ApiResponse.success(response);
    }
}
