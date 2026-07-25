package com.example.v_o_server.domain.group.entity;

import com.example.v_o_server.common.entity.BaseTimeEntity;
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
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "private_groups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrivateGroup extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private GroupTheme theme;

    @Column(name = "name", nullable = false, length = 15)
    private String name;

    @Column(name = "group_image_url", length = 1000)
    private String groupImageUrl;

    @Column(name = "group_image_object_key", length = 500)
    private String groupImageObjectKey;

    @Column(name = "notification_start_time", nullable = false)
    private LocalTime notificationStartTime;

    @Column(name = "notification_end_time", nullable = false)
    private LocalTime notificationEndTime;

    @Column(name = "timezone", nullable = false, length = 60)
    private String timezone;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GroupStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private PrivateGroup(User owner, GroupTheme theme, String name, String groupImageUrl,
            String groupImageObjectKey, LocalTime notificationStartTime, LocalTime notificationEndTime,
            String timezone, Integer maxMembers, GroupStatus status) {
        this.owner = owner;
        this.theme = theme;
        this.name = name;
        this.groupImageUrl = groupImageUrl;
        this.groupImageObjectKey = groupImageObjectKey;
        this.notificationStartTime = notificationStartTime;
        this.notificationEndTime = notificationEndTime;
        this.timezone = timezone;
        this.maxMembers = maxMembers;
        this.status = status;
    }

    public boolean isActive() {
        return status == GroupStatus.ACTIVE;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateImage(String groupImageUrl, String groupImageObjectKey) {
        this.groupImageUrl = groupImageUrl;
        this.groupImageObjectKey = groupImageObjectKey;
    }

    public void changeOwner(User newOwner) {
        this.owner = newOwner;
    }

    /** 소프트 삭제. */
    public void softDelete(LocalDateTime deletedAt) {
        this.status = GroupStatus.DELETED;
        this.deletedAt = deletedAt;
    }
}
