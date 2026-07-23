package com.example.v_o_server.domain.auth.entity;

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
@Table(name = "auth_oauth_accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthOauthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private OauthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(name = "provider_email")
    private String providerEmail;

    @Column(name = "provider_email_verified")
    private Boolean providerEmailVerified;

    @Column(name = "provider_display_name")
    private String providerDisplayName;

    @Column(name = "provider_profile_image_url")
    private String providerProfileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OauthAccountStatus status;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "unlinked_at")
    private LocalDateTime unlinkedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    private AuthOauthAccount(OauthProvider provider, String providerUserId, String providerEmail,
            Boolean providerEmailVerified, String providerDisplayName, String providerProfileImageUrl,
            OauthAccountStatus status, User user) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
        this.providerEmailVerified = providerEmailVerified;
        this.providerDisplayName = providerDisplayName;
        this.providerProfileImageUrl = providerProfileImageUrl;
        this.status = status;
        this.user = user;
    }

    public void recordLogin(LocalDateTime now) {
        if (this.linkedAt == null) {
            this.linkedAt = now;
        }
        this.lastLoginAt = now;
    }

    public void unlink(LocalDateTime now) {
        this.status = OauthAccountStatus.UNLINKED;
        this.unlinkedAt = now;
    }

    public void relink(LocalDateTime now) {
        this.status = OauthAccountStatus.ACTIVE;
        this.unlinkedAt = null;
        recordLogin(now);
    }
}
