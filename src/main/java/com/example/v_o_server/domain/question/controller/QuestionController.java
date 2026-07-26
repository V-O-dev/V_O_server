package com.example.v_o_server.domain.question.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.domain.question.dto.response.DailyQuestionResponse;
import com.example.v_o_server.domain.question.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Question", description = "질문 관련 API")
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    // TODO: JWT 인증 및 그룹 멤버십 검증 추가 (인증 파트 완료 후)
    @Operation(summary = "오늘의 질문 조회", description = "그룹에 배정된 오늘의 질문을 조회합니다. 아직 배정 전이면 새로 배정합니다.")
    @GetMapping("/daily")
    public ApiResponse<DailyQuestionResponse> getDailyQuestion(@RequestParam("groupId") Long groupId) {
        return ApiResponse.success(questionService.getDailyQuestion(groupId));
    }
}
