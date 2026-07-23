package com.example.v_o_server.domain.user.entity;

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
@Table(name = "push_devices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private DevicePlatform platform;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "device_token", nullable = false)
    private String deviceToken;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Builder
    private PushDevice(User user, DevicePlatform platform, String provider, String deviceToken,
            String appVersion, Boolean isEnabled) {
        this.user = user;
        this.platform = platform;
        this.provider = provider;
        this.deviceToken = deviceToken;
        this.appVersion = appVersion;
        this.isEnabled = isEnabled;
    }

    public void updateToken(String deviceToken, String appVersion) {
        this.deviceToken = deviceToken;
        this.appVersion = appVersion;
        this.isEnabled = true;
    }
}
