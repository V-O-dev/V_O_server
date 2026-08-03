package com.example.v_o_server.domain.user.entity;

import com.example.v_o_server.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseTimeEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "profile_image_object_key")
    private String profileImageObjectKey;

    @Column(name = "onboarding_completed_at")
    private LocalDateTime onboardingCompletedAt;

    @Builder
    private UserProfile(User user, String nickname, String profileImageUrl, String profileImageObjectKey) {
        this.user = user;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.profileImageObjectKey = profileImageObjectKey;
    }

    /** 온보딩(이름·사진 입력) 완료 시각을 기록한다. 클라이언트는 이 값으로 온보딩 재진입 여부를 판단한다. */
    public void completeOnboarding(LocalDateTime now) {
        this.onboardingCompletedAt = now;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfileImage(String profileImageUrl, String profileImageObjectKey) {
        this.profileImageUrl = profileImageUrl;
        this.profileImageObjectKey = profileImageObjectKey;
    }
}
