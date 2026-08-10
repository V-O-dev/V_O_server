package com.example.v_o_server.domain.group.service;

import static com.example.v_o_server.domain.group.GroupTestFixtures.alias;
import static com.example.v_o_server.domain.group.GroupTestFixtures.group;
import static com.example.v_o_server.domain.group.GroupTestFixtures.member;
import static com.example.v_o_server.domain.group.GroupTestFixtures.profile;
import static com.example.v_o_server.domain.group.GroupTestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.v_o_server.domain.group.dto.GroupMemberResponse;
import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.GroupMemberRole;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.repository.GroupMemberAliasRepository;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.repository.UserProfileRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 이 기능의 핵심 계약인 <b>viewer 격리</b>를 조립 로직 수준에서 고정한다 —
 * 조립기가 viewer 키로 조회한 호칭만 응답에 싣고, 다른 viewer의 호칭을 섞지 않는다.
 *
 * <p><b>검증 범위 밖</b>: 리포지토리가 목이므로 "DB에 저장된 A의 호칭이 B의 조회 쿼리에 잡히지 않는다"는
 * 저장소 수준 격리는 여기서 확인되지 않는다. 그 부분은 JPQL의 {@code viewer.id = :viewerUserId} 조건과
 * 개발 DB 확인에 의존한다 (계획서 §10-5).</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GroupMemberViewAssembler")
class GroupMemberViewAssemblerTest {

    private static final Long GROUP_ID = 100L;
    private static final Long VIEWER_A = 1L;
    private static final Long VIEWER_B = 2L;
    private static final Long TARGET_C = 3L;

    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private GroupMemberAliasRepository groupMemberAliasRepository;

    private GroupMemberViewAssembler assembler;

    private User userA;
    private User userB;
    private User userC;
    private PrivateGroup group;

    @BeforeEach
    void setUp() {
        assembler = new GroupMemberViewAssembler(
                groupMemberRepository, userProfileRepository,
                new GroupMemberAliasReader(groupMemberAliasRepository));
        userA = user(VIEWER_A);
        userB = user(VIEWER_B);
        userC = user(TARGET_C);
        group = group(GROUP_ID, userA, 15);
    }

    private List<GroupMember> threeMembers() {
        LocalDateTime base = LocalDateTime.of(2026, 8, 1, 9, 0);
        return List.of(
                member(11L, group, userA, GroupMemberRole.OWNER, MemberStatus.ACTIVE, base),
                member(12L, group, userB, GroupMemberRole.MEMBER, MemberStatus.ACTIVE, base.plusDays(1)),
                member(13L, group, userC, GroupMemberRole.MEMBER, MemberStatus.ACTIVE, base.plusDays(2)));
    }

    @Test
    @DisplayName("viewer=A로 조립하면 A가 C에게 붙인 호칭이 실린다")
    void aliasIsVisibleOnlyToTheViewerWhoSetIt() {
        given(userProfileRepository.findAllById(any()))
                .willReturn(List.of(profile(userC, "김유진", "https://cdn/3.jpg")));
        given(groupMemberAliasRepository.findAliases(eq(GROUP_ID), eq(VIEWER_A), any()))
                .willReturn(List.of(alias(1L, group, userA, userC, "엄마")));

        GroupMemberResponse seenByA = assembler.assemble(GROUP_ID, VIEWER_A, threeMembers()).stream()
                .filter(m -> m.userId().equals(TARGET_C)).findFirst().orElseThrow();

        assertThat(seenByA.alias()).isEqualTo("엄마");
        assertThat(seenByA.displayName()).isEqualTo("엄마");
        assertThat(seenByA.nickname()).isEqualTo("김유진");
    }

    @Test
    @DisplayName("viewer=B로 조립하면 호칭이 비어 원래 닉네임이 표시된다")
    void otherViewerSeesTheOriginalNickname() {
        given(userProfileRepository.findAllById(any()))
                .willReturn(List.of(profile(userC, "김유진", "https://cdn/3.jpg")));
        given(groupMemberAliasRepository.findAliases(eq(GROUP_ID), eq(VIEWER_B), any()))
                .willReturn(List.of());

        GroupMemberResponse seenByB = assembler.assemble(GROUP_ID, VIEWER_B, threeMembers()).stream()
                .filter(m -> m.userId().equals(TARGET_C)).findFirst().orElseThrow();

        assertThat(seenByB.alias()).isNull();
        assertThat(seenByB.displayName()).isEqualTo("김유진");
    }

    @Test
    @DisplayName("프로필이 없는 유저는 닉네임·이미지·표시이름이 모두 null")
    void memberWithoutProfileHasNullNames() {
        given(userProfileRepository.findAllById(any())).willReturn(List.of());
        given(groupMemberAliasRepository.findAliases(eq(GROUP_ID), eq(VIEWER_A), any()))
                .willReturn(List.of());

        GroupMemberResponse response = assembler.assemble(GROUP_ID, VIEWER_A, threeMembers()).stream()
                .filter(m -> m.userId().equals(TARGET_C)).findFirst().orElseThrow();

        assertThat(response.nickname()).isNull();
        assertThat(response.profileImageUrl()).isNull();
        assertThat(response.displayName()).isNull();
    }

    @Test
    @DisplayName("방장 우선 → 가입 시각 → 멤버 ID 순으로 정렬한다")
    void sortsOwnerFirstThenJoinedAtThenId() {
        LocalDateTime sameMoment = LocalDateTime.of(2026, 8, 1, 9, 0);
        List<GroupMember> members = List.of(
                member(30L, group, userC, GroupMemberRole.MEMBER, MemberStatus.ACTIVE, sameMoment),
                member(20L, group, userB, GroupMemberRole.MEMBER, MemberStatus.ACTIVE, sameMoment),
                member(40L, group, userA, GroupMemberRole.OWNER, MemberStatus.ACTIVE, sameMoment.plusDays(9)));
        given(userProfileRepository.findAllById(any())).willReturn(List.of());
        given(groupMemberAliasRepository.findAliases(eq(GROUP_ID), eq(VIEWER_A), any()))
                .willReturn(List.of());

        List<GroupMemberResponse> result = assembler.assemble(GROUP_ID, VIEWER_A, members);

        // 방장은 가장 늦게 가입했어도 맨 앞. 그 뒤는 가입 시각이 같으니 memberId 오름차순.
        assertThat(result).extracting(GroupMemberResponse::memberId)
                .containsExactly(40L, 20L, 30L);
    }

    @Test
    @DisplayName("isMe는 호출자 본인에게만 true")
    void isMeIsTrueOnlyForTheCaller() {
        given(userProfileRepository.findAllById(any())).willReturn(List.of());
        given(groupMemberAliasRepository.findAliases(eq(GROUP_ID), eq(VIEWER_B), any()))
                .willReturn(List.of());

        List<GroupMemberResponse> result = assembler.assemble(GROUP_ID, VIEWER_B, threeMembers());

        assertThat(result).filteredOn(GroupMemberResponse::isMe)
                .extracting(GroupMemberResponse::userId)
                .containsExactly(VIEWER_B);
    }

    @Test
    @DisplayName("호칭 조회는 ACTIVE 멤버의 userId 집합으로만 좁힌다")
    void aliasLookupIsScopedToActiveMemberIds() {
        given(userProfileRepository.findAllById(any())).willReturn(List.of());
        given(groupMemberAliasRepository.findAliases(eq(GROUP_ID), eq(VIEWER_A), any()))
                .willReturn(List.of());

        assembler.assemble(GROUP_ID, VIEWER_A, threeMembers());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(groupMemberAliasRepository).findAliases(eq(GROUP_ID), eq(VIEWER_A), captor.capture());
        // 나갔던 멤버의 호칭 행이 쌓여도 조회 범위가 커지지 않아야 한다.
        assertThat(captor.getValue()).containsExactlyInAnyOrder(VIEWER_A, VIEWER_B, TARGET_C);
    }

    @Test
    @DisplayName("멤버가 없으면 쿼리를 실행하지 않고 빈 목록을 반환한다")
    void returnsEmptyWithoutQueryingWhenNoMembers() {
        assertThat(assembler.assemble(GROUP_ID, VIEWER_A, List.of())).isEmpty();
        verify(userProfileRepository, org.mockito.Mockito.never()).findAllById(any());
        verify(groupMemberAliasRepository, org.mockito.Mockito.never())
                .findAliases(any(), any(), any());
    }

    @Test
    @DisplayName("assembleActiveMembers는 ACTIVE 멤버만 조회해 조립한다")
    void assembleActiveMembersLoadsActiveOnly() {
        given(groupMemberRepository.findByGroupIdAndStatus(GROUP_ID, MemberStatus.ACTIVE))
                .willReturn(threeMembers());
        given(userProfileRepository.findAllById(any())).willReturn(List.of());
        given(groupMemberAliasRepository.findAliases(eq(GROUP_ID), eq(VIEWER_A), any()))
                .willReturn(List.of());

        assertThat(assembler.assembleActiveMembers(GROUP_ID, VIEWER_A)).hasSize(3);
        verify(groupMemberRepository).findByGroupIdAndStatus(GROUP_ID, MemberStatus.ACTIVE);
    }
}
