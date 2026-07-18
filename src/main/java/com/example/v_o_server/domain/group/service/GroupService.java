package com.example.v_o_server.domain.group.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.common.storage.GroupImageStorage;
import com.example.v_o_server.domain.group.dto.GroupCreateRequest;
import com.example.v_o_server.domain.group.dto.GroupCreateResponse;
import com.example.v_o_server.domain.group.dto.GroupDetailResponse;
import com.example.v_o_server.domain.group.dto.GroupMemberResponse;
import com.example.v_o_server.domain.group.dto.GroupNameDuplicateResponse;
import com.example.v_o_server.domain.group.dto.GroupSummaryResponse;
import com.example.v_o_server.domain.group.dto.GroupUpdateRequest;
import com.example.v_o_server.domain.group.entity.GroupMember;
import com.example.v_o_server.domain.group.entity.GroupMemberRole;
import com.example.v_o_server.domain.group.entity.GroupStatus;
import com.example.v_o_server.domain.group.entity.GroupTheme;
import com.example.v_o_server.domain.group.entity.MemberStatus;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.repository.GroupMemberRepository;
import com.example.v_o_server.domain.group.repository.GroupThemeRepository;
import com.example.v_o_server.domain.group.repository.PrivateGroupRepository;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    /** ERD 기본값. */
    private static final String DEFAULT_TIMEZONE = "Asia/Seoul";
    private static final int DEFAULT_MAX_MEMBERS = 15;

    private final PrivateGroupRepository privateGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupThemeRepository groupThemeRepository;
    private final UserRepository userRepository;
    private final GroupAccessGuard accessGuard;
    private final GroupImageStorage groupImageStorage;

    /** G1 그룹 생성 — 생성자는 OWNER 멤버로 함께 등록된다. */
    @Transactional
    public GroupCreateResponse createGroup(Long userId, GroupCreateRequest request) {
        validateTimeRange(request.notificationStartTime(), request.notificationEndTime());

        if (groupMemberRepository.existsActiveGroupNameForUser(userId, request.groupName(), null)) {
            throw new BusinessException(ErrorCode.GROUP_NAME_DUPLICATED);
        }

        User owner = getUser(userId);
        GroupTheme theme = groupThemeRepository.findByCode(request.themeCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.THEME_NOT_FOUND));

        PrivateGroup group = privateGroupRepository.save(PrivateGroup.builder()
                .owner(owner)
                .theme(theme)
                .name(request.groupName())
                .notificationStartTime(request.notificationStartTime())
                .notificationEndTime(request.notificationEndTime())
                .timezone(DEFAULT_TIMEZONE)
                .maxMembers(DEFAULT_MAX_MEMBERS)
                .status(GroupStatus.ACTIVE)
                .build());

        GroupMember ownerMember = groupMemberRepository.save(GroupMember.builder()
                .group(group)
                .user(owner)
                .role(GroupMemberRole.OWNER)
                .status(MemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build());

        GroupDetailResponse detail =
                GroupDetailResponse.of(group, List.of(GroupMemberResponse.from(ownerMember)));
        return new GroupCreateResponse(group.getId(), detail);
    }

    /** G3 그룹명 중복 확인 — 현재 사용자의 ACTIVE 그룹 범위에서만 판단한다. */
    public GroupNameDuplicateResponse checkNameDuplicated(Long userId, String name) {
        return new GroupNameDuplicateResponse(
                groupMemberRepository.existsActiveGroupNameForUser(userId, name, null));
    }

    /** G2 내 그룹 목록 조회. */
    public List<GroupSummaryResponse> getMyGroups(Long userId) {
        return groupMemberRepository.findActiveMembershipsWithGroup(userId).stream()
                .map(membership -> {
                    PrivateGroup group = membership.getGroup();
                    long memberCount =
                            groupMemberRepository.countByGroupIdAndStatus(group.getId(), MemberStatus.ACTIVE);
                    return GroupSummaryResponse.of(group, memberCount, membership.getRole());
                })
                .toList();
    }

    /** G4 그룹 상세 조회 — 멤버만 가능. */
    public GroupDetailResponse getGroupDetail(Long userId, Long groupId) {
        PrivateGroup group = accessGuard.getActiveGroup(groupId);
        accessGuard.assertMember(groupId, userId);

        List<GroupMemberResponse> members =
                groupMemberRepository.findByGroupIdAndStatus(groupId, MemberStatus.ACTIVE).stream()
                        .map(GroupMemberResponse::from)
                        .toList();
        return GroupDetailResponse.of(group, members);
    }

    /**
     * G5 그룹 정보 수정 — 멤버면 가능. 전달된 필드만 부분 수정한다.
     * 이름/이미지가 모두 없으면 수정할 내용이 없으므로 INVALID_INPUT_VALUE.
     */
    @Transactional
    public GroupDetailResponse updateGroup(Long userId, Long groupId, GroupUpdateRequest request,
                                           MultipartFile image) {
        boolean hasName = request != null && request.groupName() != null && !request.groupName().isBlank();
        boolean hasImage = image != null && !image.isEmpty();
        if (!hasName && !hasImage) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "수정할 그룹명 또는 이미지가 필요합니다.");
        }

        PrivateGroup group = accessGuard.getActiveGroup(groupId);
        accessGuard.assertMember(groupId, userId);

        if (hasName) {
            // 자기 자신은 중복 검사에서 제외 — 같은 이름을 그대로 다시 보내도 통과해야 한다.
            if (groupMemberRepository.existsActiveGroupNameForUser(userId, request.groupName(), groupId)) {
                throw new BusinessException(ErrorCode.GROUP_NAME_DUPLICATED);
            }
            group.updateName(request.groupName());
        }
        if (hasImage) {
            GroupImageStorage.StoredImage stored = groupImageStorage.store(image);
            group.updateImage(stored.url(), stored.objectKey());
        }

        List<GroupMemberResponse> members =
                groupMemberRepository.findByGroupIdAndStatus(groupId, MemberStatus.ACTIVE).stream()
                        .map(GroupMemberResponse::from)
                        .toList();
        return GroupDetailResponse.of(group, members);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateTimeRange(java.time.LocalTime start, java.time.LocalTime end) {
        if (!start.isBefore(end)) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE,
                    "알림 시작 시간은 종료 시간보다 앞서야 합니다.");
        }
    }
}
