package com.example.v_o_server.domain.group.service;

import com.example.v_o_server.domain.group.entity.GroupInvite;
import com.example.v_o_server.domain.group.entity.InviteStatus;
import com.example.v_o_server.domain.group.repository.GroupInviteRepository;
import com.example.v_o_server.domain.group.repository.PrivateGroupRepository;
import com.example.v_o_server.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 초대 코드 insert를 시도당 독립 트랜잭션으로 수행한다.
 *
 * <p>{@code invite_code}의 UNIQUE 충돌 시 PostgreSQL은 트랜잭션을 abort 상태로 만들기 때문에,
 * 같은 트랜잭션 안에서 코드를 바꿔 재시도할 수 없다. 시도마다 트랜잭션을 분리해야
 * 충돌한 시도만 롤백되고 다음 시도가 정상 동작한다.</p>
 */
@Component
@RequiredArgsConstructor
public class GroupInviteWriter {

    private final GroupInviteRepository groupInviteRepository;
    private final PrivateGroupRepository privateGroupRepository;
    private final UserRepository userRepository;

    /**
     * @throws DataIntegrityViolationException 코드가 이미 존재하면 (호출자가 재시도 판단)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GroupInvite save(Long groupId, Long creatorUserId, String code, String inviteUrl,
                            LocalDateTime expiresAt) {
        return groupInviteRepository.saveAndFlush(GroupInvite.builder()
                // 호출자가 이미 그룹·멤버십을 검증했으므로 FK 세팅에는 프록시 참조로 충분하다.
                .group(privateGroupRepository.getReferenceById(groupId))
                .createdBy(userRepository.getReferenceById(creatorUserId))
                .inviteCode(code)
                .inviteUrl(inviteUrl)
                .usedCount(0)
                .expiresAt(expiresAt)
                .status(InviteStatus.ACTIVE)
                .build());
    }
}
