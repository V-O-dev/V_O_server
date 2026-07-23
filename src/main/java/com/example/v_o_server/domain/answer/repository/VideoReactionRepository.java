package com.example.v_o_server.domain.answer.repository;

import com.example.v_o_server.domain.answer.entity.VideoReaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoReactionRepository extends JpaRepository<VideoReaction, Long> {

    Optional<VideoReaction> findByVideo_IdAndUser_Id(Long videoId, Long userId);

    boolean existsByVideo_IdAndUser_Id(Long videoId, Long userId);

    long countByVideo_Id(Long videoId);
}
