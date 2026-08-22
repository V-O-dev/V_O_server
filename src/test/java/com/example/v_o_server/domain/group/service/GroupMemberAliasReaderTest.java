package com.example.v_o_server.domain.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.v_o_server.domain.group.entity.GroupMemberAlias;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.repository.GroupMemberAliasRepository;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.entity.UserStatus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * HomeFeedService는 {@link GroupMemberAliasReader}를 목으로 두고 검증하므로, 여기서 다루는
 * {@code findAliasesByGroup}의 실제 그룹핑 로직(그룹 → 대상 순으로 키를 두는 것) 자체는
 * 이 클래스에서만 검증된다. outer/inner 키를 바꿔치기하는 실수는 여기가 아니면 잡히지 않는다.
 */
@DisplayName("GroupMemberAliasReader.findAliasesByGroup")
class GroupMemberAliasReaderTest {

    private GroupMemberAliasRepository repository;
    private GroupMemberAliasReader reader;

    @BeforeEach
    void setUp() {
        repository = mock(GroupMemberAliasRepository.class);
        reader = new GroupMemberAliasReader(repository);
    }

    @Test
    @DisplayName("반환 맵의 바깥쪽 키는 그룹ID, 안쪽 키는 대상 userId다 (뒤바뀌면 이 테스트가 실패한다)")
    void groupsAliasesByGroupIdThenTargetUserId() {
        PrivateGroup groupA = group(10L);
        PrivateGroup groupB = group(20L);
        given(repository.findAliasesInGroups(any(), any(), any())).willReturn(List.of(
                alias(groupA, 2L, "동생"),
                alias(groupB, 3L, "선배")
        ));

        Map<Long, Map<Long, String>> result = reader.findAliasesByGroup(List.of(10L, 20L), 1L, List.of(2L, 3L));

        assertThat(result.keySet()).containsExactlyInAnyOrder(10L, 20L);
        assertThat(result.get(10L)).containsExactly(Map.entry(2L, "동생"));
        assertThat(result.get(20L)).containsExactly(Map.entry(3L, "선배"));
    }

    @Test
    @DisplayName("같은 대상이 그룹 A·B에 모두 있고 A에서만 호칭이 있으면, B 쪽 맵에는 그 대상 키 자체가 없다")
    void doesNotLeakAliasIntoOtherGroupForSameTarget() {
        PrivateGroup groupA = group(10L);
        given(repository.findAliasesInGroups(any(), any(), any()))
                .willReturn(List.of(alias(groupA, 2L, "동생")));

        Map<Long, Map<Long, String>> result = reader.findAliasesByGroup(List.of(10L, 20L), 1L, List.of(2L));

        assertThat(result).containsOnlyKeys(10L);
        assertThat(result.get(10L)).containsEntry(2L, "동생");
    }

    @Test
    @DisplayName("대상 집합이 비어 있으면 쿼리를 아예 호출하지 않고 빈 맵을 돌려준다")
    void skipsQueryWhenTargetUserIdsEmpty() {
        Map<Long, Map<Long, String>> result = reader.findAliasesByGroup(List.of(10L), 1L, List.of());

        assertThat(result).isEmpty();
        verify(repository, never()).findAliasesInGroups(any(), any(), any());
    }

    private PrivateGroup group(Long id) {
        PrivateGroup group = PrivateGroup.builder().name("그룹" + id).build();
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    private GroupMemberAlias alias(PrivateGroup group, Long targetUserId, String aliasText) {
        User viewer = user(1L);
        User target = user(targetUserId);
        return GroupMemberAlias.builder()
                .group(group)
                .viewer(viewer)
                .target(target)
                .alias(aliasText)
                .build();
    }

    private User user(Long id) {
        User user = User.builder().status(UserStatus.ACTIVE).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
