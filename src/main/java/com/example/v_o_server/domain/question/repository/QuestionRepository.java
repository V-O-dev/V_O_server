package com.example.v_o_server.domain.question.repository;

import com.example.v_o_server.domain.question.entity.Question;
import com.example.v_o_server.domain.question.entity.QuestionStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("""
            select q
            from Question q
            join QuestionThemeMap qtm on qtm.question = q
            where qtm.theme.id = :themeId
              and q.status = :status
            """)
    List<Question> findByThemeIdAndStatus(@Param("themeId") Long themeId, @Param("status") QuestionStatus status);
}
