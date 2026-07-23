package com.example.v_o_server.domain.answer.repository;

import com.example.v_o_server.domain.answer.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
}
