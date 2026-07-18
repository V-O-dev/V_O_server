package com.example.v_o_server.domain.group.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.domain.group.dto.GroupCreateRequest;
import com.example.v_o_server.domain.group.dto.GroupCreateResponse;
import com.example.v_o_server.domain.group.dto.GroupDetailResponse;
import com.example.v_o_server.domain.group.dto.GroupJoinRequest;
import com.example.v_o_server.domain.group.dto.GroupJoinResponse;
import com.example.v_o_server.domain.group.dto.GroupNameDuplicateResponse;
import com.example.v_o_server.domain.group.dto.GroupSummaryResponse;
import com.example.v_o_server.domain.group.dto.GroupUpdateRequest;
import com.example.v_o_server.domain.group.dto.InviteCodeResponse;
import com.example.v_o_server.domain.group.dto.OwnerTransferRequest;
import com.example.v_o_server.domain.group.service.GroupInviteService;
import com.example.v_o_server.domain.group.service.GroupMemberService;
import com.example.v_o_server.domain.group.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Group", description = "그룹 API")
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
@Validated
public class GroupController {

    private final GroupService groupService;
    private final GroupInviteService groupInviteService;
    private final GroupMemberService groupMemberService;

    @Operation(summary = "그룹 생성", description = "새 그룹을 만들고 생성자를 방장으로 등록합니다.")
    @PostMapping
    public ApiResponse<GroupCreateResponse> createGroup(@Valid @RequestBody GroupCreateRequest request) {
        // TODO: 인증 완성되면 @AuthenticationPrincipal 등으로 실제 userId 추출하도록 교체
        Long tempUserId = 1L;
        return ApiResponse.success(groupService.createGroup(tempUserId, request));
    }

    @Operation(summary = "내 그룹 목록 조회", description = "현재 사용자가 활동 중인 그룹 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<GroupSummaryResponse>> getMyGroups() {
        // TODO: 인증 완성되면 @AuthenticationPrincipal 등으로 실제 userId 추출하도록 교체
        Long tempUserId = 1L;
        return ApiResponse.success(groupService.getMyGroups(tempUserId));
    }

    @Operation(summary = "그룹명 중복 확인", description = "현재 사용자의 활동 그룹 중 동일한 이름이 있는지 확인합니다.")
    @GetMapping("/check-duplicate")
    public ApiResponse<GroupNameDuplicateResponse> checkDuplicate(
            @RequestParam @NotBlank(message = "그룹명은 필수입니다.") String name) {
        // TODO: 인증 완성되면 @AuthenticationPrincipal 등으로 실제 userId 추출하도록 교체
        Long tempUserId = 1L;
        return ApiResponse.success(groupService.checkNameDuplicated(tempUserId, name));
    }

    @Operation(summary = "그룹 상세 조회", description = "그룹 상세 정보와 멤버 목록을 조회합니다. 멤버만 조회할 수 있습니다.")
    @GetMapping("/{groupId}")
    public ApiResponse<GroupDetailResponse> getGroupDetail(@PathVariable Long groupId) {
        // TODO: 인증 완성되면 @AuthenticationPrincipal 등으로 실제 userId 추출하도록 교체
        Long tempUserId = 1L;
        return ApiResponse.success(groupService.getGroupDetail(tempUserId, groupId));
    }

    @Operation(summary = "그룹 정보 수정", description = "그룹명 또는 이미지를 수정합니다. 최소 한 개 필드는 필요합니다.")
    @PatchMapping(value = "/{groupId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<GroupDetailResponse> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestPart(value = "request", required = false) GroupUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        // TODO: 인증 완성되면 @AuthenticationPrincipal 등으로 실제 userId 추출하도록 교체
        Long tempUserId = 1L;
        return ApiResponse.success(groupService.updateGroup(tempUserId, groupId, request, image));
    }

    @Operation(summary = "초대 코드 발급", description = "24시간 유효한 초대 코드를 발급합니다. 멤버만 발급할 수 있습니다.")
    @PostMapping("/{groupId}/invite-code")
    public ApiResponse<InviteCodeResponse> issueInviteCode(@PathVariable Long groupId) {
        // TODO: 인증 완성되면 @AuthenticationPrincipal 등으로 실제 userId 추출하도록 교체
        Long tempUserId = 1L;
        return ApiResponse.success(groupInviteService.issueInviteCode(tempUserId, groupId));
    }

    @Operation(summary = "초대 코드로 가입", description = "초대 코드로 그룹에 가입합니다.")
    @PostMapping("/join")
    public ApiResponse<GroupJoinResponse> joinGroup(@Valid @RequestBody GroupJoinRequest request) {
        // TODO: 인증 완성되면 @AuthenticationPrincipal 등으로 실제 userId 추출하도록 교체
        Long tempUserId = 1L;
        return ApiResponse.success(groupInviteService.joinByInviteCode(tempUserId, request.code()));
    }

    @Operation(summary = "멤버 강제 퇴장", description = "방장이 멤버를 강제 퇴장시킵니다.")
    @DeleteMapping("/{groupId}/members/{memberId}")
    public ApiResponse<Void> kickMember(@PathVariable Long groupId, @PathVariable Long memberId) {
        // TODO: 인증 완성되면 @AuthenticationPrincipal 등으로 실제 userId 추출하도록 교체
        Long tempUserId = 1L;
        groupMemberService.kickMember(tempUserId, groupId, memberId);
        return ApiResponse.ok();
    }

    @Operation(summary = "방장 권한 위임", description = "방장이 다른 활동 멤버에게 권한을 위임합니다.")
    @PatchMapping("/{groupId}/owner")
    public ApiResponse<Void> transferOwner(@PathVariable Long groupId,
                                           @Valid @RequestBody OwnerTransferRequest request) {
        // TODO: 인증 완성되면 @AuthenticationPrincipal 등으로 실제 userId 추출하도록 교체
        Long tempUserId = 1L;
        groupMemberService.transferOwner(tempUserId, groupId, request.newOwnerId());
        return ApiResponse.ok();
    }

    @Operation(summary = "그룹 나가기", description = "현재 사용자가 그룹을 나갑니다. 방장은 먼저 권한을 위임해야 합니다.")
    @DeleteMapping("/{groupId}/members/me")
    public ApiResponse<Void> leaveGroup(@PathVariable Long groupId) {
        // TODO: 인증 완성되면 @AuthenticationPrincipal 등으로 실제 userId 추출하도록 교체
        Long tempUserId = 1L;
        groupMemberService.leaveGroup(tempUserId, groupId);
        return ApiResponse.ok();
    }
}
