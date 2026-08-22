package com.example.v_o_server.domain.group.service;

import com.example.v_o_server.domain.group.entity.GroupMemberAlias;
import com.example.v_o_server.domain.group.repository.GroupMemberAliasRepository;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * "나만 보는 호칭" 조회 공통 로직.
 *
 * <p>그룹 멤버 목록뿐 아니라 피드·댓글에서도 같은 규칙으로 표시 이름을 만들어야 하므로
 * 조회와 fallback 규칙을 여기 한 곳에 모았다. 세 화면이 각자 fallback을 구현하면 어긋난다.</p>
 */
@Component
@RequiredArgsConstructor
public class GroupMemberAliasReader {

    private final GroupMemberAliasRepository groupMemberAliasRepository;

    /**
     * viewer가 이 그룹에서 지정한 호칭을 {@code 대상 userId -> 호칭}으로 반환한다.
     *
     * @param targetUserIds 조회 대상. 비어 있으면 쿼리를 아예 실행하지 않는다.
     */
    public Map<Long, String> findAliases(Long groupId, Long viewerUserId, Collection<Long> targetUserIds) {
        if (groupId == null || viewerUserId == null || targetUserIds == null || targetUserIds.isEmpty()) {
            return Map.of();
        }
        return groupMemberAliasRepository.findAliases(groupId, viewerUserId, targetUserIds).stream()
                .collect(Collectors.toMap(alias -> alias.getTarget().getId(), GroupMemberAlias::getAlias));
    }

    /**
     * 홈 피드처럼 여러 그룹을 넘나드는 화면용 — {@code 그룹ID -> (대상 userId -> 호칭)}으로 반환한다.
     *
     * <p>같은 사람이 그룹 A·B에 모두 있고 A에서만 호칭을 지정했다면, 반환 맵에서도 A 키에만 값이 실린다.
     * 호출부가 항목별로 "그 아이템이 속한 그룹의 맵"에서만 꺼내 써야 그룹 스코프가 깨지지 않는다.</p>
     *
     * @param targetUserIds 조회 대상. 비어 있으면 쿼리를 아예 실행하지 않는다.
     */
    public Map<Long, Map<Long, String>> findAliasesByGroup(
            Collection<Long> groupIds, Long viewerUserId, Collection<Long> targetUserIds) {
        if (groupIds == null || groupIds.isEmpty() || viewerUserId == null
                || targetUserIds == null || targetUserIds.isEmpty()) {
            return Map.of();
        }
        return groupMemberAliasRepository.findAliasesInGroups(groupIds, viewerUserId, targetUserIds).stream()
                .collect(Collectors.groupingBy(
                        alias -> alias.getGroup().getId(),
                        Collectors.toMap(alias -> alias.getTarget().getId(), GroupMemberAlias::getAlias)
                ));
    }

    /**
     * 화면에 그대로 찍을 표시 이름을 계산한다.
     *
     * <p>호칭이 있으면 호칭, 없으면 전역 닉네임. 온보딩 전이라 프로필이 없으면 {@code null}이며,
     * 빈 이름을 어떻게 보여줄지는 클라이언트 표시 정책이라 서버가 기본값을 만들지 않는다.</p>
     */
    public static String resolveDisplayName(String alias, String nickname) {
        return alias != null ? alias : nickname;
    }
}
