package com.example.v_o_server.domain.answer.entity;

import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.question.entity.GroupDailyQuestion;
import com.example.v_o_server.domain.question.entity.Question;
import com.example.v_o_server.domain.user.entity.User;
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
@Table(name = "daily_answers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_daily_question_id", nullable = false)
    private GroupDailyQuestion groupDailyQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private PrivateGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AnswerUploadStatus status;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Builder
    private DailyAnswer(GroupDailyQuestion groupDailyQuestion, PrivateGroup group, User user, Question question,
            LocalDate serviceDate, AnswerUploadStatus status) {
        this.groupDailyQuestion = groupDailyQuestion;
        this.group = group;
        this.user = user;
        this.question = question;
        this.serviceDate = serviceDate;
        this.status = status;
    }

    public boolean isUploaded() {
        return status == AnswerUploadStatus.UPLOADED;
    }

    public void markUploaded(LocalDateTime uploadedAt) {
        this.status = AnswerUploadStatus.UPLOADED;
        this.uploadedAt = uploadedAt;
    }
}
