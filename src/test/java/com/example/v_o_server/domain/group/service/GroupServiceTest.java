package com.example.v_o_server.domain.group.service;

import static com.example.v_o_server.domain.group.GroupTestFixtures.group;
import static com.example.v_o_server.domain.group.GroupTestFixtures.member;
import static com.example.v_o_server.domain.group.GroupTestFixtures.theme;
import static com.example.v_o_server.domain.group.GroupTestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.storage.FileStorageService;
import com.example.v_o_server.domain.group.dto.GroupCreateRequest;
import com.example.v_o_server.domain.group.dto.GroupCreateResponse;
import com.example.v_o_server.domain.group.dto.GroupDetailResponse;
import com.example.v_o_server.domain.group.dto.GroupMemberResponse;
import com.example.v_o_server.domain.group.dto.GroupUpdateRequest;
import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.GroupMemberRole;
import com.example.v_o_server.domain.group.entity.GroupStatus;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.repository.GroupInviteRepository;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import com.example.v_o_server.domain.group.repository.GroupThemeRepository;
import com.example.v_o_server.domain.group.repository.PrivateGroupRepository;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupService")
class GroupServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long GROUP_ID = 100L;

    @Mock
    private PrivateGroupRepository privateGroupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private GroupInviteRepository groupInviteRepository;
    @Mock
    private GroupThemeRepository groupThemeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupAccessGuard accessGuard;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private GroupMemberViewAssembler memberViewAssembler;

    @InjectMocks
    private GroupService groupService;

    private GroupCreateRequest createRequest(String name, LocalTime start, LocalTime end) {
        return new GroupCreateRequest(name, "FAMILY", start, end);
    }

    /**
     * 조립기가 돌려주는 방장 멤버 뷰. 조립 규칙(프로필·호칭 병합)은 GroupMemberViewAssemblerTest가 검증하므로
     * 여기서는 GroupService가 조립기 결과를 그대로 흘려보내는지만 본다.
     */
    private GroupMemberResponse ownerMemberView(PrivateGroup group, User owner) {
        return GroupMemberResponse.of(
                member(1L, group, owner, GroupMemberRole.OWNER, MemberStatus.ACTIVE),
                "홍길동", null, null, "홍길동", true);
    }

    @Nested
    @DisplayName("그룹 생성")
    class CreateGroup {

        @Test
        @DisplayName("생성자를 OWNER 멤버로 함께 등록한다")
        void registersCreatorAsOwner() {
            User owner = user(USER_ID);
            PrivateGroup group = group(GROUP_ID, owner, 15);

            given(groupMemberRepository.existsActiveGroupNameForUser(eq(USER_ID), eq("우리 가족"), isNull()))
                    .willReturn(false);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(owner));
            given(groupThemeRepository.findByCode("FAMILY")).willReturn(Optional.of(theme(1L, "FAMILY")));
            given(privateGroupRepository.save(any(PrivateGroup.class))).willReturn(group);
            given(groupMemberRepository.save(any(GroupMember.class)))
                    .willReturn(member(1L, group, owner, GroupMemberRole.OWNER, MemberStatus.ACTIVE));
            given(memberViewAssembler.assemble(eq(GROUP_ID), eq(USER_ID), anyList()))
                    .willReturn(List.of(ownerMemberView(group, owner)));

            GroupCreateResponse response = groupService.createGroup(
                    USER_ID, createRequest("우리 가족", LocalTime.of(20, 0), LocalTime.of(21, 0)));

            assertThat(response.groupId()).isEqualTo(GROUP_ID);
            assertThat(response.group().members()).hasSize(1);
            assertThat(response.group().members().get(0).role()).isEqualTo(GroupMemberRole.OWNER);
        }

        @Test
        @DisplayName("이미지를 함께 보내면 저장 결과를 그룹에 반영한다")
        void storesImageOnCreate() {
            User owner = user(USER_ID);
            PrivateGroup group = group(GROUP_ID, owner, 15);
            MockMultipartFile image =
                    new MockMultipartFile("image", "a.png", "image/png", new byte[]{1, 2, 3});

            given(groupMemberRepository.existsActiveGroupNameForUser(eq(USER_ID), eq("우리 가족"), isNull()))
                    .willReturn(false);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(owner));
            given(groupThemeRepository.findByCode("FAMILY")).willReturn(Optional.of(theme(1L, "FAMILY")));
            given(fileStorageService.upload(eq(image), any()))
                    .willReturn(new FileStorageService.StoredFile("https://cdn/a.png", "key/a.png"));
            given(privateGroupRepository.save(any(PrivateGroup.class))).willReturn(group);
            given(groupMemberRepository.save(any(GroupMember.class)))
                    .willReturn(member(1L, group, owner, GroupMemberRole.OWNER, MemberStatus.ACTIVE));

            groupService.createGroup(USER_ID, createRequest("우리 가족", LocalTime.of(20, 0), LocalTime.of(21, 0)),
                    image);

            ArgumentCaptor<PrivateGroup> saved = ArgumentCaptor.forClass(PrivateGroup.class);
            verify(privateGroupRepository).save(saved.capture());
            assertThat(saved.getValue().getGroupImageUrl()).isEqualTo("https://cdn/a.png");
            assertThat(saved.getValue().getGroupImageObjectKey()).isEqualTo("key/a.png");
        }

        @Test
        @DisplayName("검증에 실패하면 이미지를 저장하지 않는다")
        void skipsImageStoreWhenValidationFails() {
            MockMultipartFile image =
                    new MockMultipartFile("image", "a.png", "image/png", new byte[]{1, 2, 3});
            given(groupMemberRepository.existsActiveGroupNameForUser(eq(USER_ID), eq("우리 가족"), isNull()))
                    .willReturn(true);

            assertThatThrownBy(() -> groupService.createGroup(
                    USER_ID, createRequest("우리 가족", LocalTime.of(20, 0), LocalTime.of(21, 0)), image))
                    .isInstanceOf(BusinessException.class);

            verify(fileStorageService, never()).upload(any(), any());
        }

        @Test
        @DisplayName("같은 사용자의 활동 그룹에 동일 이름이 있으면 GROUP_NAME_DUPLICATED")
        void rejectsDuplicateName() {
            given(groupMemberRepository.existsActiveGroupNameForUser(eq(USER_ID), eq("우리 가족"), isNull()))
                    .willReturn(true);

            assertThatThrownBy(() -> groupService.createGroup(
                    USER_ID, createRequest("우리 가족", LocalTime.of(20, 0), LocalTime.of(21, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.GROUP_NAME_DUPLICATED);

            verify(privateGroupRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 테마면 THEME_NOT_FOUND")
        void rejectsUnknownTheme() {
            given(groupMemberRepository.existsActiveGroupNameForUser(any(), any(), isNull())).willReturn(false);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
            given(groupThemeRepository.findByCode("FAMILY")).willReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.createGroup(
                    USER_ID, createRequest("우리 가족", LocalTime.of(20, 0), LocalTime.of(21, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.THEME_NOT_FOUND);
        }

        @Test
        @DisplayName("시작과 종료 시간이 같으면 INVALID_TIME_RANGE")
        void rejectsEqualTimeRange() {
            assertThatThrownBy(() -> groupService.createGroup(
                    USER_ID, createRequest("우리 가족", LocalTime.of(21, 0), LocalTime.of(21, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TIME_RANGE);

            verify(privateGroupRepository, never()).save(any());
        }

        @Test
        @DisplayName("자정을 넘기는 시간대(종료<시작)도 허용한다")
        void allowsOvernightTimeRange() {
            User owner = user(USER_ID);
            PrivateGroup group = group(GROUP_ID, owner, 15);

            given(groupMemberRepository.existsActiveGroupNameForUser(eq(USER_ID), eq("우리 가족"), isNull()))
                    .willReturn(false);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(owner));
            given(groupThemeRepository.findByCode("FAMILY")).willReturn(Optional.of(theme(1L, "FAMILY")));
            given(privateGroupRepository.save(any(PrivateGroup.class))).willReturn(group);
            given(groupMemberRepository.save(any(GroupMember.class)))
                    .willReturn(member(1L, group, owner, GroupMemberRole.OWNER, MemberStatus.ACTIVE));

            // 20:00 부터 다음 날 10:00 까지 — 자정 넘김
            GroupCreateResponse response = groupService.createGroup(
                    USER_ID, createRequest("우리 가족", LocalTime.of(20, 0), LocalTime.of(10, 0)));

            assertThat(response.groupId()).isEqualTo(GROUP_ID);
            verify(privateGroupRepository).save(any(PrivateGroup.class));
        }
    }

    @Nested
    @DisplayName("그룹 정보 수정")
    class UpdateGroup {

        @Test
        @DisplayName("그룹명·테마·이미지가 모두 없으면 INVALID_INPUT_VALUE")
        void rejectsEmptyUpdate() {
            assertThatThrownBy(() ->
                    groupService.updateGroup(USER_ID, GROUP_ID, new GroupUpdateRequest(null, null), null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }

        @Test
        @DisplayName("중복 검사에서 대상 그룹 자신은 제외한다")
        void excludesSelfFromDuplicateCheck() {
            User owner = user(USER_ID);
            PrivateGroup group = group(GROUP_ID, owner, 15);

            given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group);
            given(groupMemberRepository.existsActiveGroupNameForUser(USER_ID, "새 이름", GROUP_ID))
                    .willReturn(false);
            given(memberViewAssembler.assembleActiveMembers(GROUP_ID, USER_ID))
                    .willReturn(List.of(ownerMemberView(group, owner)));

            groupService.updateGroup(USER_ID, GROUP_ID, new GroupUpdateRequest("새 이름", null), null);

            assertThat(group.getName()).isEqualTo("새 이름");
            verify(groupMemberRepository).existsActiveGroupNameForUser(USER_ID, "새 이름", GROUP_ID);
        }

        @Test
        @DisplayName("이미지만 보내면 이름은 그대로 두고 이미지 저장 결과를 반영한다")
        void updatesImageOnly() {
            User owner = user(USER_ID);
            PrivateGroup group = group(GROUP_ID, owner, 15);
            MockMultipartFile image =
                    new MockMultipartFile("image", "a.png", "image/png", new byte[]{1, 2, 3});

            given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group);
            given(fileStorageService.upload(eq(image), any()))
                    .willReturn(new FileStorageService.StoredFile("https://cdn/a.png", "key/a.png"));
            given(memberViewAssembler.assembleActiveMembers(GROUP_ID, USER_ID))
                    .willReturn(List.of(ownerMemberView(group, owner)));

            groupService.updateGroup(USER_ID, GROUP_ID, new GroupUpdateRequest(null, null), image);

            assertThat(group.getName()).isEqualTo("테스트 그룹");
            assertThat(group.getGroupImageUrl()).isEqualTo("https://cdn/a.png");
            assertThat(group.getGroupImageObjectKey()).isEqualTo("key/a.png");
        }

        @Test
        @DisplayName("테마 코드만 보내면 그룹 테마를 변경한다")
        void updatesThemeOnly() {
            User owner = user(USER_ID);
            PrivateGroup group = group(GROUP_ID, owner, 15);

            given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group);
            given(groupThemeRepository.findByCode("COUPLE"))
                    .willReturn(Optional.of(theme(2L, "COUPLE")));
            given(memberViewAssembler.assembleActiveMembers(GROUP_ID, USER_ID))
                    .willReturn(List.of(ownerMemberView(group, owner)));

            groupService.updateGroup(USER_ID, GROUP_ID, new GroupUpdateRequest(null, "COUPLE"), null);

            assertThat(group.getTheme().getCode()).isEqualTo("COUPLE");
            assertThat(group.getName()).isEqualTo("테스트 그룹");
        }

        @Test
        @DisplayName("존재하지 않는 테마 코드면 THEME_NOT_FOUND, 이미지는 저장하지 않는다")
        void rejectsUnknownThemeAndSkipsImage() {
            User owner = user(USER_ID);
            PrivateGroup group = group(GROUP_ID, owner, 15);
            MockMultipartFile image =
                    new MockMultipartFile("image", "a.png", "image/png", new byte[]{1, 2, 3});

            given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group);
            given(groupThemeRepository.findByCode("NOPE")).willReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.updateGroup(
                    USER_ID, GROUP_ID, new GroupUpdateRequest(null, "NOPE"), image))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.THEME_NOT_FOUND);

            verify(fileStorageService, never()).upload(any(), any());
        }
    }

    @Nested
    @DisplayName("그룹 삭제")
    class DeleteGroup {

        @Test
        @DisplayName("방장이 삭제하면 그룹은 DELETED, 남은 멤버는 전원 정리되고 초대도 무효화된다")
        void deletesGroupWithMembersAndInvites() {
            User owner = user(USER_ID);
            PrivateGroup group = group(GROUP_ID, owner, 15);

            given(privateGroupRepository.findByIdAndStatusForUpdate(GROUP_ID, GroupStatus.ACTIVE))
                    .willReturn(Optional.of(group));
            given(accessGuard.assertOwner(GROUP_ID, USER_ID))
                    .willReturn(member(1L, group, owner, GroupMemberRole.OWNER, MemberStatus.ACTIVE));

            groupService.deleteGroup(USER_ID, GROUP_ID);

            assertThat(group.getStatus()).isEqualTo(GroupStatus.DELETED);
            assertThat(group.getDeletedAt()).isNotNull();
            // 강제 퇴장이 아니라 LEFT로 정리해야 재가입 차단 대상이 되지 않는다.
            verify(groupMemberRepository).leaveAllActiveMembers(eq(GROUP_ID), any(LocalDateTime.class));
            verify(groupInviteRepository).revokeAllActiveInvites(GROUP_ID);
        }

        @Test
        @DisplayName("방장이 아니면 NOT_GROUP_OWNER — 아무것도 지우지 않는다")
        void rejectsNonOwner() {
            User owner = user(USER_ID);
            PrivateGroup group = group(GROUP_ID, owner, 15);

            given(privateGroupRepository.findByIdAndStatusForUpdate(GROUP_ID, GroupStatus.ACTIVE))
                    .willReturn(Optional.of(group));
            given(accessGuard.assertOwner(GROUP_ID, USER_ID))
                    .willThrow(new BusinessException(ErrorCode.NOT_GROUP_OWNER));

            assertThatThrownBy(() -> groupService.deleteGroup(USER_ID, GROUP_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.NOT_GROUP_OWNER);

            assertThat(group.getStatus()).isEqualTo(GroupStatus.ACTIVE);
            verify(groupMemberRepository, never()).leaveAllActiveMembers(any(), any());
            verify(groupInviteRepository, never()).revokeAllActiveInvites(any());
        }

        @Test
        @DisplayName("이미 삭제됐거나 없는 그룹이면 GROUP_NOT_FOUND")
        void rejectsMissingGroup() {
            given(privateGroupRepository.findByIdAndStatusForUpdate(GROUP_ID, GroupStatus.ACTIVE))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.deleteGroup(USER_ID, GROUP_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.GROUP_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("그룹 상세 조회")
    class GetGroupDetail {

        private static final Long OTHER_USER_ID = 2L;
        private static final Long TARGET_USER_ID = 3L;

        /**
         * 호칭 격리의 서비스 계층 계약: 상세 조회는 <b>호출자를 그대로 viewer로</b> 조립기에 넘겨야 한다.
         * 여기서 viewer가 섞이면 남의 호칭이 노출된다.
         */
        @Test
        @DisplayName("호출자별로 다른 viewer로 조립해 A와 B가 서로 다른 호칭을 본다")
        void passesCallerAsViewerSoAliasesStayIsolated() {
            User owner = user(USER_ID);
            PrivateGroup group = group(GROUP_ID, owner, 15);
            User target = user(TARGET_USER_ID);
            GroupMember targetMember =
                    member(13L, group, target, GroupMemberRole.MEMBER, MemberStatus.ACTIVE);

            given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group);
            given(memberViewAssembler.assembleActiveMembers(GROUP_ID, USER_ID)).willReturn(List.of(
                    GroupMemberResponse.of(targetMember, "김유진", null, "엄마", "엄마", false)));
            given(memberViewAssembler.assembleActiveMembers(GROUP_ID, OTHER_USER_ID)).willReturn(List.of(
                    GroupMemberResponse.of(targetMember, "김유진", null, null, "김유진", false)));

            GroupDetailResponse seenByA = groupService.getGroupDetail(USER_ID, GROUP_ID);
            GroupDetailResponse seenByB = groupService.getGroupDetail(OTHER_USER_ID, GROUP_ID);

            assertThat(seenByA.members().get(0).displayName()).isEqualTo("엄마");
            assertThat(seenByB.members().get(0).displayName()).isEqualTo("김유진");
            assertThat(seenByB.members().get(0).alias()).isNull();

            verify(memberViewAssembler).assembleActiveMembers(GROUP_ID, USER_ID);
            verify(memberViewAssembler).assembleActiveMembers(GROUP_ID, OTHER_USER_ID);
        }

        @Test
        @DisplayName("멤버가 아니면 NOT_GROUP_MEMBER — 조립기를 호출하지 않는다")
        void rejectsNonMemberBeforeAssembling() {
            given(accessGuard.getActiveGroup(GROUP_ID)).willReturn(group(GROUP_ID, user(USER_ID), 15));
            willThrow(new BusinessException(ErrorCode.NOT_GROUP_MEMBER))
                    .given(accessGuard).assertMember(GROUP_ID, USER_ID);

            assertThatThrownBy(() -> groupService.getGroupDetail(USER_ID, GROUP_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.NOT_GROUP_MEMBER);
            verify(memberViewAssembler, never()).assembleActiveMembers(any(), any());
        }
    }
}
