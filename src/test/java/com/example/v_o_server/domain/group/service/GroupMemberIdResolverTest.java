package com.example.v_o_server.domain.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.GroupMemberRole;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.entity.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * HomeFeedService는 {@link GroupMemberIdResolver}를 목으로 두고 검증하므로, 여기서 다루는
 * {@code findMemberIdsByGroup}의 실제 그룹핑·중복 병합 로직 자체는 이 클래스에서만 검증된다.
 */
@DisplayName("GroupMemberIdResolver.findMemberIdsByGroup")
class GroupMemberIdResolverTest {

    private GroupMemberRepository repository;
    private GroupMemberIdResolver resolver;

    @BeforeEach
    void setUp() {
        repository = mock(GroupMemberRepository.class);
        resolver = new GroupMemberIdResolver(repository);
    }

    @Test
    @DisplayName("반환 맵의 바깥쪽 키는 그룹ID, 안쪽 키는 대상 userId, 값은 group_members.id다 (뒤바뀌면 이 테스트가 실패한다)")
    void groupsMemberIdsByGroupIdThenUserId() {
        PrivateGroup groupA = group(10L);
        PrivateGroup groupB = group(20L);
        given(repository.findByGroupIdInAndStatusAndUserIdIn(any(), any(), any())).willReturn(List.of(
                member(21L, groupA, 2L, LocalDateTime.of(2026, 1, 1, 0, 0)),
                member(33L, groupB, 3L, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        Map<Long, Map<Long, Long>> result = resolver.findMemberIdsByGroup(List.of(10L, 20L), List.of(2L, 3L));

        assertThat(result.keySet()).containsExactlyInAnyOrder(10L, 20L);
        assertThat(result.get(10L)).containsExactly(Map.entry(2L, 21L));
        assertThat(result.get(20L)).containsExactly(Map.entry(3L, 33L));
    }

    @Test
    @DisplayName("같은 그룹에 같은 유저의 ACTIVE 멤버십 행이 중복돼도, id가 작은 쪽이 채택된다 (순서 무관)")
    void picksSmallerMemberIdOnDuplicateRowsRegardlessOfOrder() {
        PrivateGroup groupA = group(10L);
        // 순서 A: 큰 id(99)가 먼저 온다.
        given(repository.findByGroupIdInAndStatusAndUserIdIn(any(), any(), any())).willReturn(List.of(
                member(99L, groupA, 2L, LocalDateTime.of(2026, 1, 1, 0, 0)),
                member(21L, groupA, 2L, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));
        assertThat(resolver.findMemberIdsByGroup(List.of(10L), List.of(2L)).get(10L)).containsEntry(2L, 21L);

        // 순서 B: 작은 id(21)가 먼저 와도 결과는 같아야 한다 — "나중 값이 이긴다" 같은 틀린 구현이면 여기서 깨진다.
        given(repository.findByGroupIdInAndStatusAndUserIdIn(any(), any(), any())).willReturn(List.of(
                member(21L, groupA, 2L, LocalDateTime.of(2026, 1, 1, 0, 0)),
                member(99L, groupA, 2L, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));
        assertThat(resolver.findMemberIdsByGroup(List.of(10L), List.of(2L)).get(10L)).containsEntry(2L, 21L);
    }

    @Test
    @DisplayName("대상 userId 집합이 비어 있으면 쿼리를 아예 호출하지 않고 빈 맵을 돌려준다")
    void skipsQueryWhenUserIdsEmpty() {
        Map<Long, Map<Long, Long>> result = resolver.findMemberIdsByGroup(List.of(10L), List.of());

        assertThat(result).isEmpty();
        verify(repository, never()).findByGroupIdInAndStatusAndUserIdIn(any(), any(), any());
    }

    private PrivateGroup group(Long id) {
        PrivateGroup group = PrivateGroup.builder().name("그룹" + id).build();
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    private GroupMember member(Long memberId, PrivateGroup group, Long userId, LocalDateTime joinedAt) {
        User user = User.builder().status(UserStatus.ACTIVE).build();
        ReflectionTestUtils.setField(user, "id", userId);
        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .role(GroupMemberRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .joinedAt(joinedAt)
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
