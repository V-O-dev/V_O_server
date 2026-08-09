package com.example.v_o_server.domain.question.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.domain.question.dto.response.DailyQuestionResponse;
import com.example.v_o_server.domain.question.dto.response.UnansweredQuestionResponse;
import com.example.v_o_server.domain.question.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @Operation(summary = "오늘의 질문 조회", description = "그룹에 배정된 오늘의 질문을 조회합니다. 아직 배정 전이면 새로 배정합니다.")
    @GetMapping("/daily")
    public ApiResponse<DailyQuestionResponse> getDailyQuestion(
            @AuthenticationPrincipal Long userId,
            @RequestParam("groupId") Long groupId
    ) {
        return ApiResponse.success(questionService.getDailyQuestion(userId, groupId));
    }

    @Operation(summary = "답변 대기 중 질문 조회",
            description = "내가 속한 그룹 중 오늘 아직 답변하지 않은 그룹의 오늘의 질문 목록을 조회합니다. "
                    + "아직 배정 전인 그룹은 이 시점에 새로 배정합니다.")
    @GetMapping("/unanswered")
    public ApiResponse<List<UnansweredQuestionResponse>> getUnansweredQuestions(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(questionService.getUnansweredQuestions(userId));
    }
}
