package com.example.v_o_server.domain.user.entity;

import com.example.v_o_server.common.entity.BaseTimeEntity;
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
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "daily_question_notification_enabled", nullable = false)
    private Boolean dailyQuestionNotificationEnabled;

    @Column(name = "interaction_notification_enabled", nullable = false)
    private Boolean interactionNotificationEnabled;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Builder
    private User(UserStatus status, Boolean dailyQuestionNotificationEnabled,
            Boolean interactionNotificationEnabled) {
        this.status = status;
        this.dailyQuestionNotificationEnabled = dailyQuestionNotificationEnabled;
        this.interactionNotificationEnabled = interactionNotificationEnabled;
    }

    public void updateLastLoginAt(LocalDateTime now) {
        this.lastLoginAt = now;
    }

    public void withdraw(LocalDateTime now) {
        this.status = UserStatus.WITHDRAWN;
        this.withdrawnAt = now;
    }

    public boolean isWithdrawn() {
        return this.status == UserStatus.WITHDRAWN;
    }

    /**
     * 탈퇴했던 계정으로 다시 로그인했을 때 되살린다.
     *
     * <p>이 처리가 없으면 탈퇴 이력이 있는 사용자는 재로그인해도 계정이 WITHDRAWN으로 남아
     * 모든 API가 차단된다.</p>
     */
    public void reactivate() {
        this.status = UserStatus.ACTIVE;
        this.withdrawnAt = null;
    }

    public void updateNotificationSettings(Boolean dailyQuestionNotificationEnabled, Boolean interactionNotificationEnabled) {
        this.dailyQuestionNotificationEnabled = dailyQuestionNotificationEnabled;
        this.interactionNotificationEnabled = interactionNotificationEnabled;
    }
}
