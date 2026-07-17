package com.example.v_o_server.domain.group.entity;

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
@Table(name = "group_invites")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private PrivateGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "invite_code", nullable = false, unique = true)
    private String inviteCode;

    @Column(name = "invite_url")
    private String inviteUrl;

    @Column(name = "qr_image_url")
    private String qrImageUrl;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InviteStatus status;

    @Builder
    private GroupInvite(PrivateGroup group, User createdBy, String inviteCode, String inviteUrl,
            String qrImageUrl, Integer usedCount, LocalDateTime expiresAt, InviteStatus status) {
        this.group = group;
        this.createdBy = createdBy;
        this.inviteCode = inviteCode;
        this.inviteUrl = inviteUrl;
        this.qrImageUrl = qrImageUrl;
        this.usedCount = usedCount;
        this.expiresAt = expiresAt;
        this.status = status;
    }
}
