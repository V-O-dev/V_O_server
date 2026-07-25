package com.example.v_o_server.domain.group.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.GroupStatus;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import com.example.v_o_server.domain.group.repository.PrivateGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 그룹 접근 권한 검증 공통 로직.
 *
 * <p>권한 실패는 일반 A002(ACCESS_DENIED) 대신 도메인 에러코드
 * ({@code NOT_GROUP_MEMBER}/{@code NOT_GROUP_OWNER})로 응답한다.</p>
 */
@Component
@RequiredArgsConstructor
public class GroupAccessGuard {

    private final PrivateGroupRepository privateGroupRepository;
    private final GroupMemberRepository groupMemberRepository;

    /** ACTIVE 그룹을 조회한다. 없거나 삭제됐으면 GROUP_NOT_FOUND. */
    public PrivateGroup getActiveGroup(Long groupId) {
        return privateGroupRepository.findByIdAndStatus(groupId, GroupStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
    }

    /** 현재 사용자의 ACTIVE 멤버십을 조회한다. 아니면 NOT_GROUP_MEMBER. */
    public GroupMember getActiveMembership(Long groupId, Long userId) {
        return groupMemberRepository.findByGroupIdAndUserIdAndStatus(groupId, userId, MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_GROUP_MEMBER));
    }

    /** 멤버 여부만 검증한다. */
    public void assertMember(Long groupId, Long userId) {
        getActiveMembership(groupId, userId);
    }

    /** 방장 여부를 검증하고 방장 멤버십을 반환한다. 멤버가 아니면 NOT_GROUP_MEMBER, 멤버지만 방장이 아니면 NOT_GROUP_OWNER. */
    public GroupMember assertOwner(Long groupId, Long userId) {
        GroupMember membership = getActiveMembership(groupId, userId);
        if (!membership.isOwner()) {
            throw new BusinessException(ErrorCode.NOT_GROUP_OWNER);
        }
        return membership;
    }
}
