package com.example.v_o_server.domain.user.repository;

import com.example.v_o_server.domain.user.entity.DevicePlatform;
import com.example.v_o_server.domain.user.entity.PushDevice;
import com.example.v_o_server.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {

    Optional<PushDevice> findByUserAndPlatform(User user, DevicePlatform platform);

    Optional<PushDevice> findByDeviceToken(String deviceToken);

    /** 푸시 발송 대상. 한 사용자가 여러 기기(폰·웹)를 등록할 수 있으므로 활성 기기를 모두 조회한다. */
    List<PushDevice> findAllByUser_IdAndIsEnabledTrue(Long userId);
}