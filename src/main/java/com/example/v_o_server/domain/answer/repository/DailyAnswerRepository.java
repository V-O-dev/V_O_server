package com.example.v_o_server.domain.answer.repository;

import com.example.v_o_server.domain.answer.entity.AnswerUploadStatus;
import com.example.v_o_server.domain.answer.entity.DailyAnswer;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyAnswerRepository extends JpaRepository<DailyAnswer, Long> {

    Optional<DailyAnswer> findByGroupIdAndUserIdAndServiceDate(Long groupId, Long userId, LocalDate serviceDate);

    boolean existsByGroupDailyQuestion_IdAndUser_IdAndStatus(
            Long groupDailyQuestionId, Long userId, AnswerUploadStatus status);

    /** 홈 피드용 — 그룹별 잠금 상태를 쿼리 1번으로 확보한다 (그룹 수만큼 반복 조회하지 않는다). */
    List<DailyAnswer> findByUserIdAndServiceDateAndGroupIdIn(
            Long userId, LocalDate serviceDate, Collection<Long> groupIds);
}
