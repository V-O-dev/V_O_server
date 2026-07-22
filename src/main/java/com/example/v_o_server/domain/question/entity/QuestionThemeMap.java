package com.example.v_o_server.domain.question.entity;

import com.example.v_o_server.common.entity.BaseCreatedAtEntity;
import com.example.v_o_server.domain.group.entity.GroupTheme;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "question_theme_maps")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionThemeMap extends BaseCreatedAtEntity {

    @EmbeddedId
    private QuestionThemeMapId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("themeId")
    @JoinColumn(name = "theme_id", nullable = false)
    private GroupTheme theme;

    @Builder
    private QuestionThemeMap(Question question, GroupTheme theme) {
        this.question = question;
        this.theme = theme;
        this.id = new QuestionThemeMapId(question.getId(), theme.getId());
    }
}
