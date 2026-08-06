package com.example.v_o_server.domain.group.service;

import static com.example.v_o_server.domain.group.GroupTestFixtures.group;
import static com.example.v_o_server.domain.group.GroupTestFixtures.invite;
import static com.example.v_o_server.domain.group.GroupTestFixtures.member;
import static com.example.v_o_server.domain.group.GroupTestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.qr.QrCodeGenerator;
import com.example.v_o_server.domain.group.dto.GroupJoinResponse;
import com.example.v_o_server.domain.group.dto.InviteCodeResponse;
import com.example.v_o_server.domain.group.entity.GroupInvite;
import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.GroupMemberRole;
import com.example.v_o_server.domain.group.entity.GroupStatus;
import com.example.v_o_server.domain.group.entity.InviteStatus;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.repository.GroupInviteRepository;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import com.example.v_o_server.domain.group.repository.PrivateGroupRepository;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupInviteService")
class GroupInviteServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long JOINER_ID = 2L;
    private static final Long GROUP_ID = 100L;
    private static final String CODE = "A3F9K2";

    @Mock
    private PrivateGroupRepository privateGroupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private GroupInviteRepository groupInviteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupAccessGuard accessGuard;
    @Mock
    private InviteCodeGenerator inviteCodeGenerator;
    @Mock
    private GroupInviteWriter groupInviteWriter;
    @Mock
    private QrCodeGenerator qrCodeGenerator;

    @InjectMocks
    private GroupInviteService groupInviteService;

    @BeforeEach
    void setUp() {
        // @Value 필드는 단위 테스트에서 주입되지 않으므로 직접 채운다.
        ReflectionTestUtils.setField(groupInviteService, "inviteBaseUrl", "https://v-o.app/invites");
    }

    private GroupInvite usableInvite(PrivateGroup group) {
        return invite(1L, group, user(OWNER_ID), CODE, LocalDateTime.now().plusHours(24), InviteStatus.ACTIVE);
    }

    @Nested
    @DisplayName("초대 코드 발급")
    class IssueInviteCode {

        @Test
        @DisplayName("발급에 성공하면 코드와 초대 링크를 반환한다")
        void issuesCode() {
            PrivateGroup group = group(GROUP_ID, user(OWNER_ID), 15);
            given(inviteCodeGenerator.generate()).willReturn(CODE);
            given(groupInviteWriter.save(eq(GROUP_ID), eq(OWNER_ID), eq(CODE), any(), any()))
                    .willReturn(usableInvite(group));

            InviteCodeResponse response = groupInviteService.issueInviteCode(OWNER_ID, GROUP_ID);

            assertThat(response.code()).isEqualTo(CODE);
            assertThat(response.inviteUrl()).isEqualTo("https://v-o.app/invites/" + CODE);
        }

        @Test
        @DisplayName("코드가 충돌하면 새 코드로 재시도한다")
        void retriesOnCodeCollision() {
            PrivateGroup group = group(GROUP_ID, user(OWNER_ID), 15);
            given(inviteCodeGenerator.generate()).willReturn("AAAAAA", CODE);
            given(groupInviteWriter.save(eq(GROUP_ID), eq(OWNER_ID), eq("AAAAAA"), any(), any()))
                    .willThrow(new DataIntegrityViolationException("duplicate invite_code"));
            given(groupInviteWriter.save(eq(GROUP_ID), eq(OWNER_ID), eq(CODE), any(), any()))
                    .willReturn(usableInvite(group));

            InviteCodeResponse response = groupInviteService.issueInviteCode(OWNER_ID, GROUP_ID);

            assertThat(response.code()).isEqualTo(CODE);
            verify(groupInviteWriter, times(2)).save(eq(GROUP_ID), eq(OWNER_ID), any(), any(), any());
        }

        @Test
        @DisplayName("아직 유효한 코드가 있으면 새로 만들지 않고 그대로 반환한다")
        void reusesUsableCode() {
            PrivateGroup group = group(GROUP_ID, user(OWNER_ID), 15);
            given(groupInviteRepository.findFirstByGroupIdAndStatusAndExpiresAtAfterOrderByExpiresAtDesc(
                    eq(GROUP_ID), eq(InviteStatus.ACTIVE), any()))
                    .willReturn(Optional.of(usableInvite(group)));

            InviteCodeResponse response = groupInviteService.issueInviteCode(OWNER_ID, GROUP_ID);

            assertThat(response.code()).isEqualTo(CODE);
            assertThat(response.qrImageUrl()).isEqualTo("/invites/" + CODE + "/qr");
            verify(groupInviteWriter, never()).save(any(), any(), any(), any(), any());
            verify(inviteCodeGenerator, never()).generate();
        }

        @Test
        @DisplayName("5회 연속 충돌하면 INTERNAL_SERVER_ERROR")
        void failsAfterMaxAttempts() {
            given(inviteCodeGenerator.generate()).willReturn("AAAAAA");
            given(groupInviteWriter.save(eq(GROUP_ID), eq(OWNER_ID), any(), any(), any()))
                    .willThrow(new DataIntegrityViolationException("duplicate invite_code"));

            assertThatThrownBy(() -> groupInviteService.issueInviteCode(OWNER_ID, GROUP_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);

            verify(groupInviteWriter, times(5)).save(eq(GROUP_ID), eq(OWNER_ID), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("초대 검증")
    class ValidateInvite {

        @Test
        @DisplayName("만료된 초대면 INVITE_EXPIRED")
        void rejectsExpiredInvite() {
            PrivateGroup group = group(GROUP_ID, user(OWNER_ID), 15);
            GroupInvite expired = invite(1L, group, user(OWNER_ID), CODE,
                    LocalDateTime.now().minusMinutes(1), InviteStatus.ACTIVE);
            given(groupInviteRepository.findByInviteCode(CODE)).willReturn(Optional.of(expired));

            assertThatThrownBy(() -> groupInviteService.getInviteInfo(CODE))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVITE_EXPIRED);
        }

        @Test
        @DisplayName("삭제된 그룹의 미만료 초대는 INVITE_NOT_FOUND")
        void rejectsInviteOfDeletedGroup() {
            PrivateGroup group = group(GROUP_ID, user(OWNER_ID), 15);
            group.softDelete(LocalDateTime.now());
            given(groupInviteRepository.findByInviteCode(CODE)).willReturn(Optional.of(usableInvite(group)));

            assertThatThrownBy(() -> groupInviteService.getInviteInfo(CODE))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVITE_NOT_FOUND);
        }

        @Test
        @DisplayName("존재하지 않는 코드면 INVITE_NOT_FOUND")
        void rejectsUnknownCode() {
            given(groupInviteRepository.findByInviteCode(CODE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> groupInviteService.getInviteInfo(CODE))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVITE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("초대 코드로 가입")
    class Join {

        @Test
        @DisplayName("신규 사용자는 MEMBER로 등록되고 초대 사용 횟수가 증가한다")
        void joinsAsNewMember() {
            PrivateGroup group = group(GROUP_ID, user(OWNER_ID), 15);
            GroupInvite invite = usableInvite(group);
            User joiner = user(JOINER_ID);

            given(groupInviteRepository.findByInviteCode(CODE)).willReturn(Optional.of(invite));
            given(privateGroupRepository.findByIdAndStatusForUpdate(GROUP_ID, GroupStatus.ACTIVE))
                    .willReturn(Optional.of(group));
            given(groupMemberRepository.countByGroupIdAndStatus(GROUP_ID, MemberStatus.ACTIVE)).willReturn(3L);
            given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, JOINER_ID)).willReturn(Optional.empty());
            given(userRepository.findById(JOINER_ID)).willReturn(Optional.of(joiner));

            GroupJoinResponse response = groupInviteService.joinByInviteCode(JOINER_ID, CODE);

            assertThat(response.groupId()).isEqualTo(GROUP_ID);
            assertThat(invite.getUsedCount()).isEqualTo(1);
            verify(groupMemberRepository).save(any(GroupMember.class));
        }

        @Test
        @DisplayName("정원이 가득 차면 GROUP_FULL")
        void rejectsWhenFull() {
            PrivateGroup group = group(GROUP_ID, user(OWNER_ID), 15);

            given(groupInviteRepository.findByInviteCode(CODE)).willReturn(Optional.of(usableInvite(group)));
            given(privateGroupRepository.findByIdAndStatusForUpdate(GROUP_ID, GroupStatus.ACTIVE))
                    .willReturn(Optional.of(group));
            given(groupMemberRepository.countByGroupIdAndStatus(GROUP_ID, MemberStatus.ACTIVE)).willReturn(15L);

            assertThatThrownBy(() -> groupInviteService.joinByInviteCode(JOINER_ID, CODE))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.GROUP_FULL);

            verify(groupMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 ACTIVE 멤버면 ALREADY_GROUP_MEMBER")
        void rejectsExistingActiveMember() {
            PrivateGroup group = group(GROUP_ID, user(OWNER_ID), 15);
            GroupMember active =
                    member(11L, group, user(JOINER_ID), GroupMemberRole.MEMBER, MemberStatus.ACTIVE);

            given(groupInviteRepository.findByInviteCode(CODE)).willReturn(Optional.of(usableInvite(group)));
            given(privateGroupRepository.findByIdAndStatusForUpdate(GROUP_ID, GroupStatus.ACTIVE))
                    .willReturn(Optional.of(group));
            given(groupMemberRepository.countByGroupIdAndStatus(GROUP_ID, MemberStatus.ACTIVE)).willReturn(3L);
            given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, JOINER_ID))
                    .willReturn(Optional.of(active));

            assertThatThrownBy(() -> groupInviteService.joinByInviteCode(JOINER_ID, CODE))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_GROUP_MEMBER);
        }

        @Test
        @DisplayName("강제 퇴장된 사용자는 유효한 코드를 받아도 KICKED_FROM_GROUP")
        void rejectsKickedMember() {
            PrivateGroup group = group(GROUP_ID, user(OWNER_ID), 15);
            GroupMember kicked = member(11L, group, user(JOINER_ID), GroupMemberRole.MEMBER, MemberStatus.ACTIVE);
            kicked.kick(OWNER_ID, LocalDateTime.now().minusDays(1));

            given(groupInviteRepository.findByInviteCode(CODE)).willReturn(Optional.of(usableInvite(group)));
            given(privateGroupRepository.findByIdAndStatusForUpdate(GROUP_ID, GroupStatus.ACTIVE))
                    .willReturn(Optional.of(group));
            given(groupMemberRepository.countByGroupIdAndStatus(GROUP_ID, MemberStatus.ACTIVE)).willReturn(3L);
            given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, JOINER_ID))
                    .willReturn(Optional.of(kicked));

            assertThatThrownBy(() -> groupInviteService.joinByInviteCode(JOINER_ID, CODE))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.KICKED_FROM_GROUP);

            assertThat(kicked.getStatus()).isEqualTo(MemberStatus.KICKED);
            verify(groupMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("나갔던 사용자는 기존 멤버십 행을 재활성화한다 (새 행 insert 아님)")
        void reactivatesLeftMembership() {
            PrivateGroup group = group(GROUP_ID, user(OWNER_ID), 15);
            GroupMember left = member(11L, group, user(JOINER_ID), GroupMemberRole.MEMBER, MemberStatus.ACTIVE);
            left.leave(LocalDateTime.now().minusDays(1));

            given(groupInviteRepository.findByInviteCode(CODE)).willReturn(Optional.of(usableInvite(group)));
            given(privateGroupRepository.findByIdAndStatusForUpdate(GROUP_ID, GroupStatus.ACTIVE))
                    .willReturn(Optional.of(group));
            given(groupMemberRepository.countByGroupIdAndStatus(GROUP_ID, MemberStatus.ACTIVE)).willReturn(3L);
            given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, JOINER_ID)).willReturn(Optional.of(left));

            groupInviteService.joinByInviteCode(JOINER_ID, CODE);

            assertThat(left.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(left.getLeftAt()).isNull();
            assertThat(left.getKickedBy()).isNull();
            verify(groupMemberRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("초대 QR 이미지")
    class InviteQr {

        @Test
        @DisplayName("유효한 코드면 초대 링크를 인코딩한 PNG를 반환한다")
        void generatesPng() {
            PrivateGroup group = group(GROUP_ID, user(OWNER_ID), 15);
            byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
            given(groupInviteRepository.findByInviteCode(CODE)).willReturn(Optional.of(usableInvite(group)));
            given(qrCodeGenerator.generatePng("https://v-o.app/invites/" + CODE, 512)).willReturn(png);

            assertThat(groupInviteService.getInviteQrImage(CODE, null)).isEqualTo(png);
        }

        @Test
        @DisplayName("허용 범위를 벗어난 size면 조회 전에 INVALID_INPUT_VALUE")
        void rejectsOutOfRangeSize() {
            assertThatThrownBy(() -> groupInviteService.getInviteQrImage(CODE, 4096))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

            verify(qrCodeGenerator, never()).generatePng(any(), org.mockito.ArgumentMatchers.anyInt());
        }

        @Test
        @DisplayName("만료된 코드면 INVITE_EXPIRED — QR을 만들지 않는다")
        void rejectsExpiredCode() {
            PrivateGroup group = group(GROUP_ID, user(OWNER_ID), 15);
            GroupInvite expired = invite(1L, group, user(OWNER_ID), CODE,
                    LocalDateTime.now().minusMinutes(1), InviteStatus.ACTIVE);
            given(groupInviteRepository.findByInviteCode(CODE)).willReturn(Optional.of(expired));

            assertThatThrownBy(() -> groupInviteService.getInviteQrImage(CODE, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVITE_EXPIRED);

            verify(qrCodeGenerator, never()).generatePng(any(), org.mockito.ArgumentMatchers.anyInt());
        }
    }
}
