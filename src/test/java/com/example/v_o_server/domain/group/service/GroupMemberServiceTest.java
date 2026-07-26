package com.example.v_o_server.domain.group.service;

import static com.example.v_o_server.domain.group.GroupTestFixtures.group;
import static com.example.v_o_server.domain.group.GroupTestFixtures.member;
import static com.example.v_o_server.domain.group.GroupTestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.GroupMemberRole;
import com.example.v_o_server.domain.group.entity.GroupStatus;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import com.example.v_o_server.domain.user.entity.User;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupMemberService")
class GroupMemberServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long GROUP_ID = 100L;

    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private GroupAccessGuard accessGuard;

    @InjectMocks
    private GroupMemberService groupMemberService;

    @Nested
    @DisplayName("멤버 강제 퇴장")
    class KickMember {

        @Test
        @DisplayName("대상 멤버를 KICKED로 바꾸고 강퇴자를 기록한다")
        void kicksMember() {
            User ownerUser = user(OWNER_ID);
            PrivateGroup group = group(GROUP_ID, ownerUser, 15);
            GroupMember ownerMember = member(10L, group, ownerUser, GroupMemberRole.OWNER, MemberStatus.ACTIVE);
            GroupMember target = member(11L, group, user(MEMBER_ID), GroupMemberRole.MEMBER, MemberStatus.ACTIVE);

            given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group);
            given(accessGuard.assertOwner(GROUP_ID, OWNER_ID)).willReturn(ownerMember);
            given(groupMemberRepository.findByIdAndGroupIdAndStatus(11L, GROUP_ID, MemberStatus.ACTIVE))
                    .willReturn(Optional.of(target));

            groupMemberService.kickMember(OWNER_ID, GROUP_ID, 11L);

            assertThat(target.getStatus()).isEqualTo(MemberStatus.KICKED);
            assertThat(target.getKickedBy()).isEqualTo(OWNER_ID);
            assertThat(target.getLeftAt()).isNotNull();
        }

        @Test
        @DisplayName("방장이 자기 자신을 강퇴하면 CANNOT_KICK_SELF")
        void rejectsSelfKick() {
            User ownerUser = user(OWNER_ID);
            PrivateGroup group = group(GROUP_ID, ownerUser, 15);
            GroupMember ownerMember = member(10L, group, ownerUser, GroupMemberRole.OWNER, MemberStatus.ACTIVE);

            given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group);
            given(accessGuard.assertOwner(GROUP_ID, OWNER_ID)).willReturn(ownerMember);
            given(groupMemberRepository.findByIdAndGroupIdAndStatus(10L, GROUP_ID, MemberStatus.ACTIVE))
                    .willReturn(Optional.of(ownerMember));

            assertThatThrownBy(() -> groupMemberService.kickMember(OWNER_ID, GROUP_ID, 10L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_KICK_SELF);
        }

        @Test
        @DisplayName("ACTIVE 멤버가 아니면 MEMBER_NOT_FOUND")
        void rejectsInactiveTarget() {
            User ownerUser = user(OWNER_ID);
            PrivateGroup group = group(GROUP_ID, ownerUser, 15);

            given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group);
            given(accessGuard.assertOwner(GROUP_ID, OWNER_ID))
                    .willReturn(member(10L, group, ownerUser, GroupMemberRole.OWNER, MemberStatus.ACTIVE));
            given(groupMemberRepository.findByIdAndGroupIdAndStatus(99L, GROUP_ID, MemberStatus.ACTIVE))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> groupMemberService.kickMember(OWNER_ID, GROUP_ID, 99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("방장 권한 위임")
    class TransferOwner {

        @Test
        @DisplayName("새 방장은 OWNER, 기존 방장은 MEMBER가 되고 그룹 소유자도 갱신된다")
        void transfersOwnership() {
            User ownerUser = user(OWNER_ID);
            User newOwnerUser = user(MEMBER_ID);
            PrivateGroup group = group(GROUP_ID, ownerUser, 15);
            GroupMember ownerMember = member(10L, group, ownerUser, GroupMemberRole.OWNER, MemberStatus.ACTIVE);
            GroupMember newOwnerMember =
                    member(11L, group, newOwnerUser, GroupMemberRole.MEMBER, MemberStatus.ACTIVE);

            given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group);
            given(accessGuard.assertOwner(GROUP_ID, OWNER_ID)).willReturn(ownerMember);
            given(groupMemberRepository.findByGroupIdAndUserIdAndStatus(GROUP_ID, MEMBER_ID, MemberStatus.ACTIVE))
                    .willReturn(Optional.of(newOwnerMember));

            groupMemberService.transferOwner(OWNER_ID, GROUP_ID, MEMBER_ID);

            assertThat(newOwnerMember.getRole()).isEqualTo(GroupMemberRole.OWNER);
            assertThat(ownerMember.getRole()).isEqualTo(GroupMemberRole.MEMBER);
            assertThat(group.getOwner().getId()).isEqualTo(MEMBER_ID);
        }

        @Test
        @DisplayName("자기 자신에게 위임하면 아무 것도 바뀌지 않는다 (no-op)")
        void selfTransferIsNoOp() {
            User ownerUser = user(OWNER_ID);
            PrivateGroup group = group(GROUP_ID, ownerUser, 15);
            GroupMember ownerMember = member(10L, group, ownerUser, GroupMemberRole.OWNER, MemberStatus.ACTIVE);

            given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group);
            given(accessGuard.assertOwner(GROUP_ID, OWNER_ID)).willReturn(ownerMember);

            groupMemberService.transferOwner(OWNER_ID, GROUP_ID, OWNER_ID);

            assertThat(ownerMember.getRole()).isEqualTo(GroupMemberRole.OWNER);
            assertThat(group.getOwner().getId()).isEqualTo(OWNER_ID);
        }
    }

    @Nested
    @DisplayName("그룹 나가기")
    class LeaveGroup {

        @Test
        @DisplayName("일반 멤버는 LEFT 처리된다")
        void memberLeaves() {
            User ownerUser = user(OWNER_ID);
            PrivateGroup group = group(GROUP_ID, ownerUser, 15);
            GroupMember membership =
                    member(11L, group, user(MEMBER_ID), GroupMemberRole.MEMBER, MemberStatus.ACTIVE);

            given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group);
            given(accessGuard.getActiveMembership(GROUP_ID, MEMBER_ID)).willReturn(membership);

            groupMemberService.leaveGroup(MEMBER_ID, GROUP_ID);

            assertThat(membership.getStatus()).isEqualTo(MemberStatus.LEFT);
            assertThat(membership.getLeftAt()).isNotNull();
            assertThat(group.getStatus()).isEqualTo(GroupStatus.ACTIVE);
        }

        @Test
        @DisplayName("다른 멤버가 남아 있는데 방장이 나가면 OWNER_CANNOT_LEAVE")
        void ownerCannotLeaveWithOtherMembers() {
            User ownerUser = user(OWNER_ID);
            PrivateGroup group = group(GROUP_ID, ownerUser, 15);
            GroupMember ownerMember = member(10L, group, ownerUser, GroupMemberRole.OWNER, MemberStatus.ACTIVE);

            given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group);
            given(accessGuard.getActiveMembership(GROUP_ID, OWNER_ID)).willReturn(ownerMember);
            given(groupMemberRepository.countByGroupIdAndStatus(GROUP_ID, MemberStatus.ACTIVE)).willReturn(3L);

            assertThatThrownBy(() -> groupMemberService.leaveGroup(OWNER_ID, GROUP_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.OWNER_CANNOT_LEAVE);

            assertThat(ownerMember.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("방장이 마지막 멤버면 그룹을 소프트 삭제하고 본인도 LEFT 처리한다")
        void lastOwnerLeavingDeletesGroup() {
            User ownerUser = user(OWNER_ID);
            PrivateGroup group = group(GROUP_ID, ownerUser, 15);
            GroupMember ownerMember = member(10L, group, ownerUser, GroupMemberRole.OWNER, MemberStatus.ACTIVE);

            given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group);
            given(accessGuard.getActiveMembership(GROUP_ID, OWNER_ID)).willReturn(ownerMember);
            given(groupMemberRepository.countByGroupIdAndStatus(GROUP_ID, MemberStatus.ACTIVE)).willReturn(1L);

            groupMemberService.leaveGroup(OWNER_ID, GROUP_ID);

            assertThat(group.getStatus()).isEqualTo(GroupStatus.DELETED);
            assertThat(group.getDeletedAt()).isNotNull();
            assertThat(ownerMember.getStatus()).isEqualTo(MemberStatus.LEFT);
        }
    }
}
