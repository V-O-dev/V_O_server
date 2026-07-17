package com.example.v_o_server.domain.archive.entity;

import com.example.v_o_server.common.entity.BaseCreatedAtEntity;
import com.example.v_o_server.domain.answer.entity.DailyAnswer;
import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.question.entity.GroupDailyQuestion;
import com.example.v_o_server.domain.question.entity.Question;
import com.example.v_o_server.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "archive_entries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArchiveEntry extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private PrivateGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_daily_question_id", nullable = false)
    private GroupDailyQuestion groupDailyQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_answer_id", nullable = false)
    private DailyAnswer dailyAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "question_content_snapshot")
    private String questionContentSnapshot;

    @Column(name = "group_name_snapshot")
    private String groupNameSnapshot;

    @Column(name = "group_theme_snapshot")
    private String groupThemeSnapshot;

    @Builder
    private ArchiveEntry(User user, PrivateGroup group, GroupDailyQuestion groupDailyQuestion,
            DailyAnswer dailyAnswer, Video video, Question question, LocalDate recordDate,
            String questionContentSnapshot, String groupNameSnapshot, String groupThemeSnapshot) {
        this.user = user;
        this.group = group;
        this.groupDailyQuestion = groupDailyQuestion;
        this.dailyAnswer = dailyAnswer;
        this.video = video;
        this.question = question;
        this.recordDate = recordDate;
        this.questionContentSnapshot = questionContentSnapshot;
        this.groupNameSnapshot = groupNameSnapshot;
        this.groupThemeSnapshot = groupThemeSnapshot;
    }
}
