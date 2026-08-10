package com.example.v_o_server.domain.group.service;

import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 사용자 ID → 그룹 멤버 ID 변환.
 *
 * <p>호칭 API 경로가 {@code group_members.id}를 요구하는데, 피드·댓글 응답은 작성자를 {@code userId}로만
 * 식별한다. 화면 정의서 FED_BLR_01("멤버 프로필 영역 탭 → 이름 편집 화면으로 이동")대로 피드에서 바로
 * 호칭을 고치려면 응답에 {@code memberId}가 있어야 해서, 두 도메인이 공유할 변환을 여기 모았다.</p>
 */
@Component
@RequiredArgsConstructor
public class GroupMemberIdResolver {

    private final GroupMemberRepository groupMemberRepository;

    /**
     * {@code 대상 userId -> group_members.id} 매핑을 반환한다.
     *
     * <p>ACTIVE 멤버만 담는다. 영상·댓글을 남긴 뒤 그룹을 나간 사람은 여기 포함되지 않아
     * 응답의 {@code memberId}가 {@code null}이 되고, 클라이언트는 그 사람에 대한 호칭 편집 진입을 막으면 된다.
     * 애초에 호칭 API도 ACTIVE 멤버가 아니면 {@code G011}로 거부한다.</p>
     */
    public Map<Long, Long> findMemberIds(Long groupId, Collection<Long> userIds) {
        if (groupId == null || userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return groupMemberRepository
                .findByGroupIdAndStatusAndUserIdIn(groupId, MemberStatus.ACTIVE, userIds).stream()
                .collect(Collectors.toMap(member -> member.getUser().getId(), GroupMember::getId));
    }
}
