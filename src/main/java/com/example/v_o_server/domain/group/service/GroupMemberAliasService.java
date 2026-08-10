package com.example.v_o_server.domain.group.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.group.dto.GroupMemberResponse;
import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.repository.GroupMemberAliasRepository;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 그룹 멤버 호칭(나만 보는 별칭) 설정·해제.
 *
 * <p>호칭은 설정한 사람에게만 보이므로, 남의 호칭을 바꿀 수 있는 권한 개념이 없다.
 * 필요한 검증은 "호출자가 이 그룹의 멤버인가"와 "대상이 이 그룹의 ACTIVE 멤버인가" 두 가지다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupMemberAliasService {

    private final GroupAccessGuard accessGuard;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMemberAliasRepository groupMemberAliasRepository;
    private final GroupMemberViewAssembler viewAssembler;

    /** 그룹 멤버 목록 조회 — 멤버만 가능. 호칭은 호출자 시점으로 붙는다. */
    public List<GroupMemberResponse> getMembers(Long viewerUserId, Long groupId) {
        accessGuard.getActiveGroup(groupId);
        accessGuard.assertMember(groupId, viewerUserId);
        return viewAssembler.assembleActiveMembers(groupId, viewerUserId);
    }

    /**
     * 호칭 설정·변경 (멱등 upsert).
     *
     * <p>{@code alias}가 비어 있으면 저장이 아니라 <b>해제</b>다 — 화면 정의서 GRP_MNG_03의
     * "공백 상태로 저장 시 원래 이름으로 롤백 처리". 전체 삭제(X)로 비우고 저장 버튼을 누르는 흐름이
     * 오류 없이 원래 이름으로 돌아가야 한다.</p>
     */
    @Transactional
    public GroupMemberResponse upsertAlias(Long viewerUserId, Long groupId, Long memberId, String alias) {
        GroupMember targetMember = resolveTargetMember(viewerUserId, groupId, memberId);
        Long targetUserId = targetMember.getUser().getId();

        // 앞뒤 공백이 섞인 값은 요청 DTO의 @Pattern에서 이미 걸러진다. 여기서는 "비었는가"만 본다.
        if (alias == null || alias.isBlank()) {
            groupMemberAliasRepository.deleteAlias(groupId, viewerUserId, targetUserId);
        } else {
            groupMemberAliasRepository.upsertAlias(groupId, viewerUserId, targetUserId, alias);
        }

        // 쓰기 쿼리가 clearAutomatically로 영속성 컨텍스트를 비우므로 targetMember는 준영속이 된다.
        // 조립기가 이 엔티티에서 읽는 값은 이미 로딩된 스칼라(id·role·joinedAt)와 프록시의 식별자뿐이라
        // 추가 초기화가 일어나지 않는다. 호칭·프로필은 조립기가 새로 조회하므로 방금 쓴 값이 반영된다.
        return viewAssembler.assembleOne(groupId, viewerUserId, targetMember);
    }

    /**
     * 호칭 해제.
     *
     * <p>호칭이 없어도 성공으로 간주한다(멱등). 다만 대상 검증은 설정과 동일하게 거친다 —
     * 검증을 건너뛰면 다른 그룹의 멤버 ID를 넣어보며 호칭 존재 여부를 탐지할 수 있다.</p>
     */
    @Transactional
    public void deleteAlias(Long viewerUserId, Long groupId, Long memberId) {
        GroupMember targetMember = resolveTargetMember(viewerUserId, groupId, memberId);
        groupMemberAliasRepository.deleteAlias(groupId, viewerUserId, targetMember.getUser().getId());
    }

    /**
     * 경로의 {@code memberId}를 검증해 대상 멤버십을 얻는다.
     *
     * <p>설정과 해제가 같은 순서·같은 에러 코드를 쓰도록 한곳에 모았다.
     * 허용 대상 집합이 두 API에서 어긋나면 안 된다.</p>
     */
    private GroupMember resolveTargetMember(Long viewerUserId, Long groupId, Long memberId) {
        accessGuard.getActiveGroup(groupId);
        accessGuard.assertMember(groupId, viewerUserId);

        GroupMember targetMember = groupMemberRepository
                .findByIdAndGroupIdAndStatus(memberId, groupId, MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 내 이름은 프로필 수정(PATCH /users/me/profile)으로 통일한다. 표시 이름이 두 군데서 갈리면 안 된다.
        if (targetMember.getUser().getId().equals(viewerUserId)) {
            throw new BusinessException(ErrorCode.ALIAS_SELF_NOT_ALLOWED);
        }
        return targetMember;
    }
}
