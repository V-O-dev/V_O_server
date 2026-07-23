package com.example.v_o_server.domain.answer.repository;

import com.example.v_o_server.domain.answer.entity.DailyAnswer;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyAnswerRepository extends JpaRepository<DailyAnswer, Long> {

    Optional<DailyAnswer> findByGroupIdAndUserIdAndServiceDate(Long groupId, Long userId, LocalDate serviceDate);
}
