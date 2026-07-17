package com.example.v_o_server.domain.question.entity;

import com.example.v_o_server.domain.group.entity.GroupTheme;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "question_theme_maps",
        uniqueConstraints = @UniqueConstraint(columnNames = {"question_id", "theme_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionThemeMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private GroupTheme theme;

    @Builder
    private QuestionThemeMap(Question question, GroupTheme theme) {
        this.question = question;
        this.theme = theme;
    }
}
