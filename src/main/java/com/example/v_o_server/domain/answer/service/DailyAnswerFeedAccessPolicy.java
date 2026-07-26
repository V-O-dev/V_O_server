package com.example.v_o_server.domain.answer.service;

import com.example.v_o_server.domain.answer.entity.AnswerUploadStatus;
import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.answer.repository.DailyAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 영상이 속한 "그룹별 오늘의 질문"에 대해 요청 사용자의 답변이 업로드 완료 상태인지로 판단한다.
 *
 * TODO: 기존에 피드 완료 여부를 판단하는 로직이 없어 새로 정의한 정책.
 *  - 가정: 같은 groupDailyQuestion에 본인 DailyAnswer(status=UPLOADED)가 있으면 접근 허용
 *  - 피드/답변 도메인 구현 시 정책이 달라지면(예: 만료된 질문은 모두 공개) 이 구현을 교체할 것
 */
@Component
@RequiredArgsConstructor
public class DailyAnswerFeedAccessPolicy implements FeedAccessPolicy {

    private final DailyAnswerRepository dailyAnswerRepository;

    @Override
    public boolean canAccess(Long userId, Video video) {
        Long groupDailyQuestionId = video.getDailyAnswer().getGroupDailyQuestion().getId();
        return dailyAnswerRepository.existsByGroupDailyQuestion_IdAndUser_IdAndStatus(
                groupDailyQuestionId, userId, AnswerUploadStatus.UPLOADED);
    }
}
