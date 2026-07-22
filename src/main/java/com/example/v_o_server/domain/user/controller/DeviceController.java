package com.example.v_o_server.domain.user.controller;

import com.example.v_o_server.common.response.ApiResponse;
import com.example.v_o_server.domain.user.dto.request.RegisterDeviceRequest;
import com.example.v_o_server.domain.user.dto.request.UnregisterDeviceRequest;
import com.example.v_o_server.domain.user.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Device", description = "디바이스 토큰(푸시 알림용) 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/devices")
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "디바이스 토큰 등록/갱신", description = "푸시 알림 발송을 위한 디바이스 토큰을 등록하거나 갱신합니다.")
    @PostMapping
    public ApiResponse<Void> registerDevice(@Valid @RequestBody RegisterDeviceRequest request) {
        Long tempUserId = 1L;
        deviceService.registerDevice(tempUserId, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "디바이스 토큰 해제", description = "등록된 디바이스 토큰을 해제합니다.")
    @DeleteMapping
    public ApiResponse<Void> unregisterDevice(@Valid @RequestBody UnregisterDeviceRequest request) {
        deviceService.unregisterDevice(request.deviceToken());
        return ApiResponse.ok();
    }
}