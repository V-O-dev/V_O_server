package com.example.v_o_server.domain.group.service;

import static com.example.v_o_server.domain.group.GroupTestFixtures.group;
import static com.example.v_o_server.domain.group.GroupTestFixtures.member;
import static com.example.v_o_server.domain.group.GroupTestFixtures.theme;
import static com.example.v_o_server.domain.group.GroupTestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.storage.FileStorageService;
import com.example.v_o_server.domain.group.dto.GroupCreateRequest;
import com.example.v_o_server.domain.group.dto.GroupCreateResponse;
import com.example.v_o_server.domain.group.dto.GroupUpdateRequest;
import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.GroupMemberRole;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import com.example.v_o_server.domain.group.repository.GroupThemeRepository;
import com.example.v_o_server.domain.group.repository.PrivateGroupRepository;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.repository.UserRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private GroupThemeRepository groupThemeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupAccessGuard accessGuard;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private GroupService groupService;

    private GroupCreateRequest createRequest(String name, LocalTime start, LocalTime end) {
        return new GroupCreateRequest(name, "FAMILY", start, end);
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

            GroupCreateResponse response = groupService.createGroup(
                    USER_ID, createRequest("우리 가족", LocalTime.of(20, 0), LocalTime.of(21, 0)));

            assertThat(response.groupId()).isEqualTo(GROUP_ID);
            assertThat(response.group().members()).hasSize(1);
            assertThat(response.group().members().get(0).role()).isEqualTo(GroupMemberRole.OWNER);
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
        @DisplayName("시작 시간이 종료 시간보다 늦으면 INVALID_TIME_RANGE")
        void rejectsInvalidTimeRange() {
            assertThatThrownBy(() -> groupService.createGroup(
                    USER_ID, createRequest("우리 가족", LocalTime.of(22, 0), LocalTime.of(21, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TIME_RANGE);

            verify(privateGroupRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("그룹 정보 수정")
    class UpdateGroup {

        @Test
        @DisplayName("그룹명과 이미지가 모두 없으면 INVALID_INPUT_VALUE")
        void rejectsEmptyUpdate() {
            assertThatThrownBy(() ->
                    groupService.updateGroup(USER_ID, GROUP_ID, new GroupUpdateRequest(null), null))
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
            given(groupMemberRepository.findByGroupIdAndStatus(GROUP_ID, MemberStatus.ACTIVE))
                    .willReturn(List.of(member(1L, group, owner, GroupMemberRole.OWNER, MemberStatus.ACTIVE)));

            groupService.updateGroup(USER_ID, GROUP_ID, new GroupUpdateRequest("새 이름"), null);

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
            given(groupMemberRepository.findByGroupIdAndStatus(GROUP_ID, MemberStatus.ACTIVE))
                    .willReturn(List.of(member(1L, group, owner, GroupMemberRole.OWNER, MemberStatus.ACTIVE)));

            groupService.updateGroup(USER_ID, GROUP_ID, new GroupUpdateRequest(null), image);

            assertThat(group.getName()).isEqualTo("테스트 그룹");
            assertThat(group.getGroupImageUrl()).isEqualTo("https://cdn/a.png");
            assertThat(group.getGroupImageObjectKey()).isEqualTo("key/a.png");
        }
    }
}
