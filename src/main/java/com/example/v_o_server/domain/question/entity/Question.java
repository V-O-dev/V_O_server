package com.example.v_o_server.domain.question.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuestionStatus status;

    @Column(name = "answer_time_limit_ms")
    private Integer answerTimeLimitMs;

    @Column(name = "use_count", nullable = false)
    private Integer useCount;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Builder
    private Question(String content, QuestionStatus status, Integer answerTimeLimitMs, Integer useCount) {
        this.content = content;
        this.status = status;
        this.answerTimeLimitMs = answerTimeLimitMs;
        this.useCount = useCount;
    }

    public void use(LocalDateTime usedAt) {
        this.useCount = this.useCount + 1;
        this.lastUsedAt = usedAt;
    }
}
