package com.example.v_o_server.domain.group.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.group.dto.GroupJoinResponse;
import com.example.v_o_server.domain.group.dto.InviteCodeResponse;
import com.example.v_o_server.domain.group.dto.InviteInfoResponse;
import com.example.v_o_server.domain.group.entity.GroupInvite;
import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.GroupMemberRole;
import com.example.v_o_server.domain.group.entity.GroupStatus;
import com.example.v_o_server.domain.group.entity.InviteStatus;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.repository.GroupInviteRepository;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import com.example.v_o_server.domain.group.repository.PrivateGroupRepository;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupInviteService {

    private static final int INVITE_VALID_HOURS = 24;
    /** 코드 UNIQUE 충돌 시 재생성 시도 횟수. */
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final PrivateGroupRepository privateGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupInviteRepository groupInviteRepository;
    private final UserRepository userRepository;
    private final GroupAccessGuard accessGuard;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final GroupInviteWriter groupInviteWriter;

    @Value("${app.invite.base-url:https://v-o.app/invites}")
    private String inviteBaseUrl;

    /**
     * G6 초대 코드 발급 — 멤버면 가능. 코드 충돌 시 재생성 재시도.
     *
     * <p>각 저장 시도는 {@link GroupInviteWriter}가 독립 트랜잭션으로 수행한다. 충돌한 시도만
     * 롤백되므로 이 메서드의 트랜잭션은 재시도 중에도 유효하다.</p>
     */
    public InviteCodeResponse issueInviteCode(Long userId, Long groupId) {
        accessGuard.getActiveGroup(groupId);
        // 멤버십이 존재한다는 것은 해당 유저가 존재한다는 뜻이므로 별도 유저 조회는 하지 않는다.
        accessGuard.assertMember(groupId, userId);

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(INVITE_VALID_HOURS);

        for (int attempt = 1; attempt <= MAX_CODE_ATTEMPTS; attempt++) {
            String code = inviteCodeGenerator.generate();
            try {
                GroupInvite invite =
                        groupInviteWriter.save(groupId, userId, code, buildInviteUrl(code), expiresAt);
                return InviteCodeResponse.from(invite);
            } catch (DataIntegrityViolationException e) {
                log.warn("초대 코드 충돌 - 재생성합니다. attempt={}/{}", attempt, MAX_CODE_ATTEMPTS);
            }
        }
        log.error("초대 코드 생성 실패 - {}회 연속 충돌. groupId={}", MAX_CODE_ATTEMPTS, groupId);
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "초대 코드 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.");
    }

    /** G7 초대 정보 조회·검증. */
    public InviteInfoResponse getInviteInfo(String code) {
        GroupInvite invite = findUsableInvite(code);
        PrivateGroup group = invite.getGroup();
        long memberCount = groupMemberRepository.countByGroupIdAndStatus(group.getId(), MemberStatus.ACTIVE);
        return InviteInfoResponse.of(group, memberCount);
    }

    /**
     * G8 초대 코드로 가입.
     *
     * <p>정원 초과를 막기 위해 그룹 행을 비관적 쓰기 락으로 잡은 뒤 멤버 수를 재확인한다.</p>
     */
    @Transactional
    public GroupJoinResponse joinByInviteCode(Long userId, String code) {
        GroupInvite invite = findUsableInvite(code);
        Long groupId = invite.getGroup().getId();

        // 락 하에서 그룹을 다시 읽어 정원 검사와 멤버 추가를 원자적으로 수행한다.
        PrivateGroup group = privateGroupRepository.findByIdAndStatusForUpdate(groupId, GroupStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_NOT_FOUND));

        long activeCount = groupMemberRepository.countByGroupIdAndStatus(groupId, MemberStatus.ACTIVE);
        if (activeCount >= group.getMaxMembers()) {
            throw new BusinessException(ErrorCode.GROUP_FULL);
        }

        Optional<GroupMember> existing = groupMemberRepository.findByGroupIdAndUserId(groupId, userId);
        if (existing.isPresent()) {
            GroupMember member = existing.get();
            if (member.isActive()) {
                throw new BusinessException(ErrorCode.ALREADY_GROUP_MEMBER);
            }
            // LEFT/KICKED 이력 행을 재활용해 재가입시킨다.
            member.rejoin(LocalDateTime.now());
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            groupMemberRepository.save(GroupMember.builder()
                    .group(group)
                    .user(user)
                    .role(GroupMemberRole.MEMBER)
                    .status(MemberStatus.ACTIVE)
                    .joinedAt(LocalDateTime.now())
                    .build());
        }

        invite.increaseUsedCount();
        return new GroupJoinResponse(group.getId(), group.getName());
    }

    /**
     * 사용 가능한 초대를 조회한다.
     * 삭제된 그룹의 미만료 초대는 무효로 본다.
     */
    private GroupInvite findUsableInvite(String code) {
        GroupInvite invite = groupInviteRepository.findByInviteCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_NOT_FOUND));

        PrivateGroup group = invite.getGroup();
        if (group == null || !group.isActive()) {
            throw new BusinessException(ErrorCode.INVITE_NOT_FOUND);
        }
        if (invite.getStatus() != InviteStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVITE_NOT_FOUND);
        }
        if (invite.isExpired(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVITE_EXPIRED);
        }
        return invite;
    }

    private String buildInviteUrl(String code) {
        String base = inviteBaseUrl.endsWith("/")
                ? inviteBaseUrl.substring(0, inviteBaseUrl.length() - 1)
                : inviteBaseUrl;
        return base + "/" + code;
    }
}
