package com.example.v_o_server.domain.notification.entity;

import com.example.v_o_server.common.entity.BaseCreatedAtEntity;
import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.question.entity.GroupDailyQuestion;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "app_notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppNotification extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body")
    private String body;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private PrivateGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_daily_question_id")
    private GroupDailyQuestion groupDailyQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    @Builder
    private AppNotification(NotificationType notificationType, String title, String body, Boolean isRead,
            User recipient, User actor, PrivateGroup group, GroupDailyQuestion groupDailyQuestion, Video video) {
        this.notificationType = notificationType;
        this.title = title;
        this.body = body;
        this.isRead = isRead;
        this.recipient = recipient;
        this.actor = actor;
        this.group = group;
        this.groupDailyQuestion = groupDailyQuestion;
        this.video = video;
    }

    public boolean isReceivedBy(Long userId) {
        return this.recipient.getId().equals(userId);
    }

    /** 이미 읽은 알림이면 readAt을 갱신하지 않는다(최초 읽은 시각 보존). */
    public void markAsRead(LocalDateTime now) {
        if (Boolean.TRUE.equals(this.isRead)) {
            return;
        }
        this.isRead = true;
        this.readAt = now;
    }
}
