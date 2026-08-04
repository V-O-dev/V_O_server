package com.example.v_o_server.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.v_o_server.domain.notification.entity.NotificationType;
import com.example.v_o_server.domain.notification.event.NotificationCreatedEvent;
import com.example.v_o_server.domain.user.entity.DevicePlatform;
import com.example.v_o_server.domain.user.entity.PushDevice;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.entity.UserStatus;
import com.example.v_o_server.domain.user.repository.PushDeviceRepository;
import com.example.v_o_server.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FCM 발송 경로 통합 테스트.
 *
 * <p>실제 FCM 서버로 요청을 보내므로 자격 증명이 있을 때만 실행된다.
 * CI에는 키가 없어 자동으로 건너뛴다.</p>
 *
 * <p>검증 방식: 형식이 잘못된 토큰을 등록해두면 FCM이 INVALID_ARGUMENT로 거절한다.
 * 즉 <b>거절당했다는 사실 자체가 요청이 FCM까지 도달했다는 증거</b>이며,
 * 그 결과로 무효 토큰이 정리되는지까지 함께 확인한다.</p>
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "FIREBASE_CREDENTIALS_BASE64", matches = ".+",
        disabledReason = "Firebase 자격 증명이 없으면 실제 발송을 확인할 수 없다")
class PushNotificationSenderTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PushDeviceRepository pushDeviceRepository;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private User testUser;

    @AfterEach
    void tearDown() {
        if (testUser != null) {
            pushDeviceRepository.findAllByUser_IdAndIsEnabledTrue(testUser.getId())
                    .forEach(pushDeviceRepository::delete);
            userRepository.delete(testUser);
        }
    }

    @Test
    @DisplayName("알림 이벤트가 커밋되면 FCM으로 발송되고, 무효한 토큰은 정리된다")
    void 무효한_토큰은_발송_시도_후_삭제된다() {
        testUser = userRepository.save(User.builder()
                .status(UserStatus.ACTIVE)
                .dailyQuestionNotificationEnabled(true)
                .interactionNotificationEnabled(true)
                .build());

        PushDevice device = pushDeviceRepository.save(PushDevice.builder()
                .user(testUser)
                .platform(DevicePlatform.WEB)
                .provider("FCM")
                .deviceToken("INVALID_TOKEN_FOR_PUSH_TEST")
                .isEnabled(true)
                .build());

        // @TransactionalEventListener(AFTER_COMMIT)는 트랜잭션 안에서 발행돼야 동작한다.
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                eventPublisher.publishEvent(new NotificationCreatedEvent(
                        testUser.getId(), NotificationType.COMMENT, "새 댓글", "테스트 알림입니다.", null)));

        assertThat(pushDeviceRepository.findById(device.getId()))
                .as("FCM이 무효 토큰을 거절하면 해당 디바이스가 삭제되어야 한다")
                .isEmpty();
    }
}
