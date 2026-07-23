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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "group_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private PrivateGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private GroupMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MemberStatus status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "kicked_by")
    private Long kickedBy;

    @Builder
    private GroupMember(PrivateGroup group, User user, GroupMemberRole role, MemberStatus status,
            LocalDateTime joinedAt) {
        this.group = group;
        this.user = user;
        this.role = role;
        this.status = status;
        this.joinedAt = joinedAt;
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public boolean isOwner() {
        return role == GroupMemberRole.OWNER;
    }

    public void changeRole(GroupMemberRole role) {
        this.role = role;
    }

    /** 본인이 그룹을 나감. */
    public void leave(LocalDateTime leftAt) {
        this.status = MemberStatus.LEFT;
        this.leftAt = leftAt;
    }

    /** 방장에 의한 강제 퇴장. */
    public void kick(Long kickedByUserId, LocalDateTime leftAt) {
        this.status = MemberStatus.KICKED;
        this.kickedBy = kickedByUserId;
        this.leftAt = leftAt;
    }

    /** LEFT/KICKED 멤버의 재가입 — 이력 행을 재사용한다. */
    public void rejoin(LocalDateTime joinedAt) {
        this.status = MemberStatus.ACTIVE;
        this.role = GroupMemberRole.MEMBER;
        this.joinedAt = joinedAt;
        this.leftAt = null;
        this.kickedBy = null;
    }
}
