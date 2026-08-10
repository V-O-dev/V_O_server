package com.example.v_o_server.domain.question.repository;

import com.example.v_o_server.domain.question.entity.Question;
import com.example.v_o_server.domain.question.entity.QuestionStatus;
import java.util.List;
import java.util.Optional;
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

    /**
     * seed 재실행 시 같은 질문이 다시 들어가지 않도록 확인한다.
     * ({@code questions.content}에는 UNIQUE 제약이 없어 애플리케이션에서 중복을 막는다)
     */
    Optional<Question> findByContent(String content);
}
