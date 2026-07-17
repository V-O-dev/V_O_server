package com.example.v_o_server.domain.question.entity;

import com.example.v_o_server.domain.group.entity.PrivateGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "group_daily_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupDailyQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private PrivateGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "question_content_snapshot", nullable = false)
    private String questionContentSnapshot;

    @Column(name = "answer_time_limit_ms")
    private Integer answerTimeLimitMs;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AssignmentStatus status;

    @Column(name = "notification_scheduled_at")
    private LocalDateTime notificationScheduledAt;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    @Builder
    private GroupDailyQuestion(PrivateGroup group, Question question, LocalDate serviceDate,
            String questionContentSnapshot, Integer answerTimeLimitMs, LocalDateTime assignedAt,
            LocalDateTime expiresAt, AssignmentStatus status, LocalDateTime notificationScheduledAt) {
        this.group = group;
        this.question = question;
        this.serviceDate = serviceDate;
        this.questionContentSnapshot = questionContentSnapshot;
        this.answerTimeLimitMs = answerTimeLimitMs;
        this.assignedAt = assignedAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.notificationScheduledAt = notificationScheduledAt;
    }
}
