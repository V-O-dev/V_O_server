package com.example.v_o_server.domain.answer.entity;

import com.example.v_o_server.common.entity.BaseTimeEntity;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "video_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoComment extends BaseTimeEntity {

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

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private VideoComment(Video video, User user, String content, Boolean isDeleted) {
        this.video = video;
        this.user = user;
        this.content = content;
        this.isDeleted = isDeleted;
    }

    public boolean isWrittenBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public void updateContent(String content, LocalDateTime now) {
        this.content = content;
        this.editedAt = now;
    }

    public void delete(LocalDateTime now) {
        this.isDeleted = true;
        this.deletedAt = now;
    }
}
