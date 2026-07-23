package com.example.v_o_server.domain.answer.repository;

import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.answer.entity.VideoStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {

    Optional<Video> findByIdAndStatus(Long id, VideoStatus status);

    @EntityGraph(attributePaths = {"user", "question"})
    Page<Video> findByGroupIdAndDailyAnswerServiceDateAndStatus(
            Long groupId,
            LocalDate serviceDate,
            VideoStatus status,
            Pageable pageable
    );
}
