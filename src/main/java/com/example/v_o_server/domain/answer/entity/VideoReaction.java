package com.example.v_o_server.domain.answer.entity;

import com.example.v_o_server.common.entity.BaseCreatedAtEntity;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "video_reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"video_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoReaction extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    private VideoReaction(Video video, User user) {
        this.video = video;
        this.user = user;
    }
}
