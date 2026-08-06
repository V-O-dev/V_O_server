package com.example.v_o_server.domain.group.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.qr.QrCodeGenerator;
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

    /** QR 한 변의 기본/허용 픽셀 크기. */
    private static final int DEFAULT_QR_SIZE = 512;
    private static final int MIN_QR_SIZE = 128;
    private static final int MAX_QR_SIZE = 1024;

    private final PrivateGroupRepository privateGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupInviteRepository groupInviteRepository;
    private final UserRepository userRepository;
    private final GroupAccessGuard accessGuard;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final GroupInviteWriter groupInviteWriter;
    private final QrCodeGenerator qrCodeGenerator;

    @Value("${app.invite.base-url:https://v-o.app/invites}")
    private String inviteBaseUrl;

    /**
     * G6 초대 코드 발급 — 멤버면 가능.
     *
     * <p>아직 유효한 코드가 있으면 그 코드를 그대로 재사용한다. 버튼을 누를 때마다 코드가 쌓이면
     * 이미 공유한 링크와 화면에 보이는 코드가 달라지기 때문이다. 유효한 코드가 없을 때만
     * 24시간짜리 새 코드를 만들고, 코드 충돌 시 재생성을 재시도한다.</p>
     *
     * <p>각 저장 시도는 {@link GroupInviteWriter}가 독립 트랜잭션으로 수행한다. 충돌한 시도만
     * 롤백되므로 이 메서드의 트랜잭션은 재시도 중에도 유효하다.</p>
     */
    public InviteCodeResponse issueInviteCode(Long userId, Long groupId) {
        accessGuard.getActiveGroup(groupId);
        // 멤버십이 존재한다는 것은 해당 유저가 존재한다는 뜻이므로 별도 유저 조회는 하지 않는다.
        accessGuard.assertMember(groupId, userId);

        LocalDateTime now = LocalDateTime.now();
        Optional<GroupInvite> reusable = groupInviteRepository
                .findFirstByGroupIdAndStatusAndExpiresAtAfterOrderByExpiresAtDesc(
                        groupId, InviteStatus.ACTIVE, now);
        if (reusable.isPresent()) {
            return InviteCodeResponse.from(reusable.get());
        }

        // 동시 요청으로 코드가 두 개 만들어져도 둘 다 유효하므로 별도 락은 걸지 않는다.
        LocalDateTime expiresAt = now.plusHours(INVITE_VALID_HOURS);

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
            // 강제 퇴장 이력은 그 자체가 블랙리스트다. 유효한 코드를 받아도 재가입시키지 않는다.
            if (member.isKicked()) {
                throw new BusinessException(ErrorCode.KICKED_FROM_GROUP);
            }
            // 스스로 나간(LEFT) 이력 행은 재활용해 재가입시킨다.
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
     * 초대 코드 QR 이미지(PNG)를 생성한다.
     *
     * <p>저장하지 않고 요청 시점에 만든다. 같은 코드에 대해 결과가 항상 같으므로
     * 컨트롤러에서 캐시 헤더를 붙여 재생성 비용을 줄인다.</p>
     *
     * @param size 한 변의 픽셀 크기 (null이면 기본값)
     */
    public byte[] getInviteQrImage(String code, Integer size) {
        int resolvedSize = resolveQrSize(size);
        GroupInvite invite = findUsableInvite(code);
        // 링크가 비어 있는 과거 데이터는 코드 문자열만 인코딩한다.
        String contents = invite.getInviteUrl() != null ? invite.getInviteUrl() : invite.getInviteCode();
        return qrCodeGenerator.generatePng(contents, resolvedSize);
    }

    private int resolveQrSize(Integer size) {
        if (size == null) {
            return DEFAULT_QR_SIZE;
        }
        if (size < MIN_QR_SIZE || size > MAX_QR_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "QR 크기는 %d~%d 픽셀이어야 합니다.".formatted(MIN_QR_SIZE, MAX_QR_SIZE));
        }
        return size;
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
