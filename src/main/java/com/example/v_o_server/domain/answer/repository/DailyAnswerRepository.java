package com.example.v_o_server.domain.answer.repository;

import com.example.v_o_server.domain.answer.entity.AnswerUploadStatus;
import com.example.v_o_server.domain.answer.entity.DailyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyAnswerRepository extends JpaRepository<DailyAnswer, Long> {

    boolean existsByGroupDailyQuestion_IdAndUser_IdAndStatus(
            Long groupDailyQuestionId, Long userId, AnswerUploadStatus status);
}
