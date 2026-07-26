package com.example.v_o_server.domain.question.repository;

import com.example.v_o_server.domain.question.entity.GroupDailyQuestion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupDailyQuestionRepository extends JpaRepository<GroupDailyQuestion, Long> {

    Optional<GroupDailyQuestion> findByGroupIdAndServiceDate(Long groupId, LocalDate serviceDate);

    @Query("""
            select new com.example.v_o_server.domain.question.repository.QuestionLastShownDate(
                gdq.question.id, max(gdq.serviceDate))
            from GroupDailyQuestion gdq
            where gdq.group.id = :groupId
            group by gdq.question.id
            """)
    List<QuestionLastShownDate> findLastShownDatesByGroupId(@Param("groupId") Long groupId);
}
