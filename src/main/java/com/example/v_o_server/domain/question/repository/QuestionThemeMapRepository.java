package com.example.v_o_server.domain.question.repository;

import com.example.v_o_server.domain.question.entity.QuestionThemeMap;
import com.example.v_o_server.domain.question.entity.QuestionThemeMapId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionThemeMapRepository extends JpaRepository<QuestionThemeMap, QuestionThemeMapId> {
}
