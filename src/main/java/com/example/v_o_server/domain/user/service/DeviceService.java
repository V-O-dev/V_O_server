package com.example.v_o_server.domain.user.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.user.dto.request.RegisterDeviceRequest;
import com.example.v_o_server.domain.user.entity.PushDevice;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.repository.PushDeviceRepository;
import com.example.v_o_server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final UserRepository userRepository;
    private final PushDeviceRepository pushDeviceRepository;

    @Transactional
    public void registerDevice(Long userId, RegisterDeviceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        pushDeviceRepository.findByUserAndPlatform(user, request.platform())
                .ifPresentOrElse(
                        existing -> existing.updateToken(request.deviceToken(), request.appVersion()),
                        () -> {
                            PushDevice newDevice = PushDevice.builder()
                                    .user(user)
                                    .platform(request.platform())
                                    .provider("FCM")
                                    .deviceToken(request.deviceToken())
                                    .appVersion(request.appVersion())
                                    .isEnabled(true)
                                    .build();
                            pushDeviceRepository.save(newDevice);
                        }
                );
    }

    @Transactional
    public void unregisterDevice(Long userId, String deviceToken) {
        pushDeviceRepository.findByDeviceToken(deviceToken)
                .filter(device -> device.getUser().getId().equals(userId))
                .ifPresent(pushDeviceRepository::delete);
        // 없는 토큰이거나 본인 소유가 아니어도 예외 없이 종료한다.
        // 응답으로 구분되지 않으므로 토큰의 존재 여부가 드러나지 않는다(멱등 처리 유지).
    }
}