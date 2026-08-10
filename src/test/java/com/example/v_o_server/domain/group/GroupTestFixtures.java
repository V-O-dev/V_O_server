package com.example.v_o_server.domain.group;

import com.example.v_o_server.domain.group.entity.GroupInvite;
import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.GroupMemberAlias;
import com.example.v_o_server.domain.group.entity.GroupMemberRole;
import com.example.v_o_server.domain.group.entity.GroupStatus;
import com.example.v_o_server.domain.group.entity.GroupTheme;
import com.example.v_o_server.domain.group.entity.InviteStatus;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.entity.UserProfile;
import com.example.v_o_server.domain.user.entity.UserStatus;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 테스트용 엔티티 픽스처.
 *
 * <p>엔티티는 id 세터를 두지 않으므로(영속화 시 DB가 채움) 테스트에서는 리플렉션으로 id를 주입한다.</p>
 */
public final class GroupTestFixtures {

    private GroupTestFixtures() {
    }

    public static User user(Long id) {
        User user = User.builder()
                .status(UserStatus.ACTIVE)
                .dailyQuestionNotificationEnabled(true)
                .interactionNotificationEnabled(true)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static GroupTheme theme(Long id, String code) {
        GroupTheme theme = GroupTheme.builder()
                .code(code)
                .name(code)
                .description(code + " 테마")
                .sortOrder(1)
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(theme, "id", id);
        return theme;
    }

    public static PrivateGroup group(Long id, User owner, int maxMembers) {
        PrivateGroup group = PrivateGroup.builder()
                .owner(owner)
                .theme(theme(1L, "FAMILY"))
                .name("테스트 그룹")
                .notificationStartTime(LocalTime.of(20, 0))
                .notificationEndTime(LocalTime.of(21, 0))
                .timezone("Asia/Seoul")
                .maxMembers(maxMembers)
                .status(GroupStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    public static GroupMember member(Long id, PrivateGroup group, User user, GroupMemberRole role,
                                     MemberStatus status) {
        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .role(role)
                .status(status)
                .joinedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    public static GroupMember member(Long id, PrivateGroup group, User user, GroupMemberRole role,
                                     MemberStatus status, LocalDateTime joinedAt) {
        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .role(role)
                .status(status)
                .joinedAt(joinedAt)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    public static GroupMemberAlias alias(Long id, PrivateGroup group, User viewer, User target, String alias) {
        GroupMemberAlias entity = GroupMemberAlias.builder()
                .group(group)
                .viewer(viewer)
                .target(target)
                .alias(alias)
                .build();
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    public static UserProfile profile(User user, String nickname, String profileImageUrl) {
        UserProfile profile = UserProfile.builder()
                .user(user)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .build();
        // @MapsId 매핑이라 영속화 시 user_id가 채워진다. 단위 테스트에서는 직접 주입한다.
        ReflectionTestUtils.setField(profile, "id", user.getId());
        return profile;
    }

    public static GroupInvite invite(Long id, PrivateGroup group, User creator, String code,
                                     LocalDateTime expiresAt, InviteStatus status) {
        GroupInvite invite = GroupInvite.builder()
                .group(group)
                .createdBy(creator)
                .inviteCode(code)
                .inviteUrl("https://v-o.app/invites/" + code)
                .usedCount(0)
                .expiresAt(expiresAt)
                .status(status)
                .build();
        ReflectionTestUtils.setField(invite, "id", id);
        return invite;
    }
}
