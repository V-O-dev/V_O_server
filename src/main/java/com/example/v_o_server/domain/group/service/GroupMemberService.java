package com.example.v_o_server.domain.group.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.GroupMemberRole;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupMemberService {

    private final GroupMemberRepository groupMemberRepository;
    private final GroupAccessGuard accessGuard;

    /** G9 멤버 강제 퇴장 — 방장만 가능. memberId는 group_members.id. */
    @Transactional
    public void kickMember(Long userId, Long groupId, Long memberId) {
        accessGuard.getActiveGroup(groupId);
        GroupMember owner = accessGuard.assertOwner(groupId, userId);

        GroupMember target = groupMemberRepository
                .findByIdAndGroupIdAndStatus(memberId, groupId, MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (target.getId().equals(owner.getId())) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_SELF);
        }
        target.kick(userId, LocalDateTime.now());
    }

    /** G10 방장 권한 위임 — 방장만 가능. 대상이 이미 본인이면 no-op. */
    @Transactional
    public void transferOwner(Long userId, Long groupId, Long newOwnerId) {
        PrivateGroup group = accessGuard.getActiveGroup(groupId);
        GroupMember currentOwner = accessGuard.assertOwner(groupId, userId);

        if (newOwnerId.equals(userId)) {
            return;
        }

        GroupMember newOwner = groupMemberRepository
                .findByGroupIdAndUserIdAndStatus(groupId, newOwnerId, MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        newOwner.changeRole(GroupMemberRole.OWNER);
        currentOwner.changeRole(GroupMemberRole.MEMBER);
        group.changeOwner(newOwner.getUser());
    }

    /**
     * G11 그룹 나가기.
     *
     * <p>방장은 다른 활동 멤버가 남아 있으면 먼저 권한을 위임해야 한다.
     * 방장이 마지막 멤버면 그룹을 소프트 삭제한다.</p>
     */
    @Transactional
    public void leaveGroup(Long userId, Long groupId) {
        PrivateGroup group = accessGuard.getActiveGroup(groupId);
        GroupMember membership = accessGuard.getActiveMembership(groupId, userId);

        LocalDateTime now = LocalDateTime.now();
        if (membership.isOwner()) {
            long activeCount = groupMemberRepository.countByGroupIdAndStatus(groupId, MemberStatus.ACTIVE);
            if (activeCount > 1) {
                throw new BusinessException(ErrorCode.OWNER_CANNOT_LEAVE);
            }
            // 방장이 마지막 멤버 — 그룹까지 정리한다.
            group.softDelete(now);
        }
        membership.leave(now);
    }
}
