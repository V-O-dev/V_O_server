package com.example.v_o_server.domain.user.repository;

import com.example.v_o_server.domain.user.entity.DevicePlatform;
import com.example.v_o_server.domain.user.entity.PushDevice;
import com.example.v_o_server.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {

    Optional<PushDevice> findByUserAndPlatform(User user, DevicePlatform platform);

    Optional<PushDevice> findByDeviceToken(String deviceToken);
}