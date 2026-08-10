package com.example.v_o_server.domain.group.service;

import static com.example.v_o_server.domain.group.GroupTestFixtures.group;
import static com.example.v_o_server.domain.group.GroupTestFixtures.member;
import static com.example.v_o_server.domain.group.GroupTestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.group.dto.GroupMemberResponse;
import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.GroupMemberRole;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.repository.GroupMemberAliasRepository;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import com.example.v_o_server.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 서비스 계층의 <b>위임·검증 계약</b>을 고정한다 — 어떤 키로 어떤 리포지토리 메서드를 부르는지, 어떤 순서로 막는지.
 *
 * <p><b>검증 범위 밖</b>: 리포지토리는 목이므로 실제 행 생성/갱신, UNIQUE 제약에 의한 행 수 유지,
 * DB에 저장된 호칭의 viewer 격리는 여기서 확인되지 않는다. 그 부분은 네이티브
 * {@code insert ... on conflict}가 실제 PostgreSQL에서 도는지로만 확인할 수 있고,
 * 이 저장소에는 통합 테스트 인프라가 없어 개발 DB에서 수동 확인이 필요하다
 * (계획서 §10-5의 {@code pg_indexes} 확인 항목).</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GroupMemberAliasService")
class GroupMemberAliasServiceTest {

    private static final Long GROUP_ID = 100L;
    private static final Long VIEWER_ID = 1L;
    private static final Long TARGET_ID = 3L;
    private static final Long TARGET_MEMBER_ID = 13L;
    private static final Long MY_MEMBER_ID = 11L;

    @Mock
    private GroupAccessGuard accessGuard;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private GroupMemberAliasRepository groupMemberAliasRepository;
    @Mock
    private GroupMemberViewAssembler viewAssembler;

    @InjectMocks
    private GroupMemberAliasService service;

    private User viewer;
    private User target;
    private PrivateGroup group;

    @BeforeEach
    void setUp() {
        viewer = user(VIEWER_ID);
        target = user(TARGET_ID);
        group = group(GROUP_ID, viewer, 15);
    }

    private GroupMember targetMember() {
        return member(TARGET_MEMBER_ID, group, target, GroupMemberRole.MEMBER, MemberStatus.ACTIVE);
    }

    private void givenTargetIsActiveMember() {
        given(groupMemberRepository.findByIdAndGroupIdAndStatus(
                TARGET_MEMBER_ID, GROUP_ID, MemberStatus.ACTIVE)).willReturn(Optional.of(targetMember()));
    }

    @Nested
    @DisplayName("호칭 설정·변경")
    class UpsertAlias {

        @Test
        @DisplayName("경로의 memberId를 target userId로 바꿔 upsert를 호출하고, 조립된 멤버를 반환한다")
        void upsertsByTargetUserId() {
            givenTargetIsActiveMember();
            GroupMemberResponse expected = GroupMemberResponse.of(
                    targetMember(), "홍길동", null, "엄마", "엄마", false);
            given(viewAssembler.assembleOne(eq(GROUP_ID), eq(VIEWER_ID), any())).willReturn(expected);

            GroupMemberResponse result =
                    service.upsertAlias(VIEWER_ID, GROUP_ID, TARGET_MEMBER_ID, "엄마");

            // 경로는 memberId지만 저장 키는 target userId다.
            verify(groupMemberAliasRepository).upsertAlias(GROUP_ID, VIEWER_ID, TARGET_ID, "엄마");
            assertThat(result.alias()).isEqualTo("엄마");
            assertThat(result.displayName()).isEqualTo("엄마");
        }

        @Test
        @DisplayName("재설정도 save가 아니라 같은 upsert 한 번으로 위임한다 (행 수 유지는 DB UNIQUE 제약이 보장)")
        void reassigningCallsUpsertAgain() {
            givenTargetIsActiveMember();
            given(viewAssembler.assembleOne(eq(GROUP_ID), eq(VIEWER_ID), any()))
                    .willReturn(GroupMemberResponse.of(targetMember(), "홍길동", null, "어머니", "어머니", false));

            service.upsertAlias(VIEWER_ID, GROUP_ID, TARGET_MEMBER_ID, "어머니");

            verify(groupMemberAliasRepository).upsertAlias(GROUP_ID, VIEWER_ID, TARGET_ID, "어머니");
            verify(groupMemberAliasRepository, never()).save(any());
        }

        @Test
        @DisplayName("자기 자신에게 지정하면 G017")
        void rejectsSelfAlias() {
            given(groupMemberRepository.findByIdAndGroupIdAndStatus(
                    MY_MEMBER_ID, GROUP_ID, MemberStatus.ACTIVE))
                    .willReturn(Optional.of(
                            member(MY_MEMBER_ID, group, viewer, GroupMemberRole.OWNER, MemberStatus.ACTIVE)));

            assertThatThrownBy(() -> service.upsertAlias(VIEWER_ID, GROUP_ID, MY_MEMBER_ID, "나"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALIAS_SELF_NOT_ALLOWED);
            verify(groupMemberAliasRepository, never()).upsertAlias(any(), any(), any(), any());
        }

        @Test
        @DisplayName("이 그룹의 ACTIVE 멤버가 아니면 G011 — 다른 그룹의 memberId도 여기서 걸린다")
        void rejectsMemberFromAnotherGroupOrInactive() {
            given(groupMemberRepository.findByIdAndGroupIdAndStatus(
                    TARGET_MEMBER_ID, GROUP_ID, MemberStatus.ACTIVE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.upsertAlias(VIEWER_ID, GROUP_ID, TARGET_MEMBER_ID, "엄마"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            verify(groupMemberAliasRepository, never()).upsertAlias(any(), any(), any(), any());
        }

        @Test
        @DisplayName("호출자가 그룹 멤버가 아니면 G003 — 대상 조회 전에 막는다")
        void rejectsNonMemberCaller() {
            willThrow(new BusinessException(ErrorCode.NOT_GROUP_MEMBER))
                    .given(accessGuard).assertMember(GROUP_ID, VIEWER_ID);

            assertThatThrownBy(() -> service.upsertAlias(VIEWER_ID, GROUP_ID, TARGET_MEMBER_ID, "엄마"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_GROUP_MEMBER);
            verify(groupMemberRepository, never())
                    .findByIdAndGroupIdAndStatus(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("삭제된 그룹이면 G001")
        void rejectsDeletedGroup() {
            willThrow(new BusinessException(ErrorCode.GROUP_NOT_FOUND))
                    .given(accessGuard).getActiveGroup(GROUP_ID);

            assertThatThrownBy(() -> service.upsertAlias(VIEWER_ID, GROUP_ID, TARGET_MEMBER_ID, "엄마"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("호칭 해제")
    class DeleteAlias {

        @Test
        @DisplayName("memberId를 target userId로 변환해 삭제한다")
        void deletesByResolvedTargetUserId() {
            givenTargetIsActiveMember();

            service.deleteAlias(VIEWER_ID, GROUP_ID, TARGET_MEMBER_ID);

            verify(groupMemberAliasRepository).deleteAlias(GROUP_ID, VIEWER_ID, TARGET_ID);
        }

        @Test
        @DisplayName("삭제 건수가 0이어도 예외 없이 통과한다 (멱등 계약)")
        void isIdempotentWhenNoAliasExists() {
            givenTargetIsActiveMember();
            given(groupMemberAliasRepository.deleteAlias(GROUP_ID, VIEWER_ID, TARGET_ID)).willReturn(0);

            service.deleteAlias(VIEWER_ID, GROUP_ID, TARGET_MEMBER_ID);

            verify(groupMemberAliasRepository).deleteAlias(GROUP_ID, VIEWER_ID, TARGET_ID);
        }

        @Test
        @DisplayName("다른 그룹의 memberId면 G011 — 호칭 존재 여부를 탐지할 수 없어야 한다")
        void rejectsMemberFromAnotherGroup() {
            given(groupMemberRepository.findByIdAndGroupIdAndStatus(
                    TARGET_MEMBER_ID, GROUP_ID, MemberStatus.ACTIVE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteAlias(VIEWER_ID, GROUP_ID, TARGET_MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            verify(groupMemberAliasRepository, never()).deleteAlias(any(), any(), any());
        }

        @Test
        @DisplayName("자기 자신 대상이면 G017 — 설정과 허용 대상이 같아야 한다")
        void rejectsSelf() {
            given(groupMemberRepository.findByIdAndGroupIdAndStatus(
                    MY_MEMBER_ID, GROUP_ID, MemberStatus.ACTIVE))
                    .willReturn(Optional.of(
                            member(MY_MEMBER_ID, group, viewer, GroupMemberRole.OWNER, MemberStatus.ACTIVE)));

            assertThatThrownBy(() -> service.deleteAlias(VIEWER_ID, GROUP_ID, MY_MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALIAS_SELF_NOT_ALLOWED);
            verify(groupMemberAliasRepository, never()).deleteAlias(any(), any(), any());
        }

        @Test
        @DisplayName("삭제 호출의 viewer 키는 항상 호출자다 — 남의 호칭 행을 지우는 호출이 나가지 않는다")
        void deletesOnlyCallersOwnAlias() {
            givenTargetIsActiveMember();

            service.deleteAlias(VIEWER_ID, GROUP_ID, TARGET_MEMBER_ID);

            // 다른 사람(TARGET_ID)이 viewer인 행을 지우는 호출은 발생하지 않는다.
            verify(groupMemberAliasRepository).deleteAlias(GROUP_ID, VIEWER_ID, TARGET_ID);
            verify(groupMemberAliasRepository, never()).deleteAlias(GROUP_ID, TARGET_ID, VIEWER_ID);
        }
    }

    @Nested
    @DisplayName("멤버 목록 조회")
    class GetMembers {

        @Test
        @DisplayName("멤버면 호출자 시점으로 조립된 목록을 반환한다")
        void returnsAssembledMembers() {
            GroupMemberResponse expected = GroupMemberResponse.of(
                    targetMember(), "홍길동", null, "엄마", "엄마", false);
            given(viewAssembler.assembleActiveMembers(GROUP_ID, VIEWER_ID)).willReturn(List.of(expected));

            assertThat(service.getMembers(VIEWER_ID, GROUP_ID)).containsExactly(expected);
            verify(accessGuard).assertMember(GROUP_ID, VIEWER_ID);
        }

        @Test
        @DisplayName("멤버가 아니면 G003")
        void rejectsNonMember() {
            willThrow(new BusinessException(ErrorCode.NOT_GROUP_MEMBER))
                    .given(accessGuard).assertMember(GROUP_ID, VIEWER_ID);

            assertThatThrownBy(() -> service.getMembers(VIEWER_ID, GROUP_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_GROUP_MEMBER);
        }
    }
}
