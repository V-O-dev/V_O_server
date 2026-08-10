package com.example.v_o_server.domain.group.service;

import com.example.v_o_server.domain.group.dto.GroupMemberResponse;
import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.GroupMemberRole;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import com.example.v_o_server.domain.user.entity.UserProfile;
import com.example.v_o_server.domain.user.repository.UserProfileRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 그룹 멤버 목록 응답 조립.
 *
 * <p>멤버 1명마다 프로필·호칭을 개별 조회하면 N+1이 되므로, 멤버 조회를 포함해 <b>쿼리 3번</b>으로 고정한다.
 * 그룹 상세·그룹 수정·그룹 생성·멤버 목록이 모두 이 조립기를 거치게 해 DTO 생성 코드가 흩어지지 않게 한다.</p>
 */
@Component
@RequiredArgsConstructor
public class GroupMemberViewAssembler {

    private final GroupMemberRepository groupMemberRepository;
    private final UserProfileRepository userProfileRepository;
    private final GroupMemberAliasReader aliasReader;

    /** 그룹의 ACTIVE 멤버 전체를 viewer 시점으로 조립한다. */
    public List<GroupMemberResponse> assembleActiveMembers(Long groupId, Long viewerUserId) {
        return assemble(groupId, viewerUserId,
                groupMemberRepository.findByGroupIdAndStatus(groupId, MemberStatus.ACTIVE));
    }

    /** 이미 로딩한 멤버 목록을 viewer 시점으로 조립한다. */
    public List<GroupMemberResponse> assemble(Long groupId, Long viewerUserId, List<GroupMember> members) {
        if (members.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = members.stream()
                .map(member -> member.getUser().getId())
                .collect(Collectors.toSet());

        Map<Long, UserProfile> profiles = userProfileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, Function.identity()));
        Map<Long, String> aliases = aliasReader.findAliases(groupId, viewerUserId, userIds);

        return members.stream()
                .sorted(memberOrder())
                .map(member -> toResponse(member, viewerUserId, profiles, aliases))
                .toList();
    }

    /** 단건 조립 — 호칭 설정/변경 응답에 쓴다. */
    public GroupMemberResponse assembleOne(Long groupId, Long viewerUserId, GroupMember member) {
        return assemble(groupId, viewerUserId, List.of(member)).get(0);
    }

    /**
     * 방장 우선 → 가입 시각 오름차순 → 멤버 ID 오름차순.
     *
     * <p>마지막 tie-breaker가 있어야 같은 초에 가입한 멤버들의 순서가 요청마다 흔들리지 않는다.</p>
     */
    private Comparator<GroupMember> memberOrder() {
        return Comparator
                .comparing((GroupMember member) -> member.getRole() == GroupMemberRole.OWNER ? 0 : 1)
                .thenComparing(GroupMember::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(GroupMember::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private GroupMemberResponse toResponse(GroupMember member, Long viewerUserId,
                                           Map<Long, UserProfile> profiles, Map<Long, String> aliases) {
        Long targetUserId = member.getUser().getId();
        UserProfile profile = profiles.get(targetUserId);
        String nickname = profile == null ? null : profile.getNickname();
        String profileImageUrl = profile == null ? null : profile.getProfileImageUrl();
        String alias = aliases.get(targetUserId);

        return GroupMemberResponse.of(
                member,
                nickname,
                profileImageUrl,
                alias,
                GroupMemberAliasReader.resolveDisplayName(alias, nickname),
                targetUserId.equals(viewerUserId));
    }
}
