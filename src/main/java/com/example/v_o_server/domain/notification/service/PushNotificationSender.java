package com.example.v_o_server.domain.notification.service;

import com.example.v_o_server.domain.notification.event.NotificationCreatedEvent;
import com.example.v_o_server.domain.user.entity.PushDevice;
import com.example.v_o_server.domain.user.repository.PushDeviceRepository;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.IncomingHttpResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

/**
 * 알림 저장이 커밋된 뒤 FCM 푸시를 발송한다.
 *
 * <p>커밋 이후에 동작하므로 발송이 실패해도 알림 저장은 롤백되지 않는다.
 * 네트워크 호출이 DB 트랜잭션을 붙잡지 않게 하려는 목적도 있다.</p>
 *
 * <p>자격 증명이 설정되지 않으면 발송을 건너뛴다. 푸시가 필요 없는 팀원은
 * 키 없이도 앱을 실행할 수 있어야 하기 때문이다.</p>
 */
@Slf4j
@Component
public class PushNotificationSender {

    private static final String FIREBASE_APP_NAME = "v-o-server";

    private final PushDeviceRepository pushDeviceRepository;
    /** 자격 증명이 없으면 null. 이 경우 발송을 건너뛴다. */
    private final FirebaseMessaging firebaseMessaging;

    public PushNotificationSender(PushDeviceRepository pushDeviceRepository,
            @Value("${firebase.credentials-base64:}") String credentialsBase64) {
        this.pushDeviceRepository = pushDeviceRepository;
        this.firebaseMessaging = initializeMessaging(credentialsBase64);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        if (firebaseMessaging == null) {
            return;
        }

        List<PushDevice> devices = pushDeviceRepository.findAllByUser_IdAndIsEnabledTrue(event.recipientId());
        for (PushDevice device : devices) {
            send(device, event);
        }
    }

    private void send(PushDevice device, NotificationCreatedEvent event) {
        try {
            firebaseMessaging.send(Message.builder()
                    .setToken(device.getDeviceToken())
                    .setNotification(Notification.builder()
                            .setTitle(event.title())
                            .setBody(event.body())
                            .build())
                    .putAllData(payloadOf(event))
                    .build());
        } catch (FirebaseMessagingException e) {
            handleSendFailure(device, e);
        } catch (RuntimeException e) {
            // 한 기기의 실패가 나머지 기기 발송을 막지 않도록 한다.
            log.error("푸시 발송 중 예외. deviceId={}", device.getId(), e);
        }
    }

    /** 클라이언트가 알림 클릭 시 이동할 화면을 판단할 수 있도록 타입과 관련 ID를 함께 싣는다. */
    private Map<String, String> payloadOf(NotificationCreatedEvent event) {
        Map<String, String> data = new HashMap<>();
        data.put("type", event.type().name());
        if (event.relatedVideoId() != null) {
            data.put("videoId", String.valueOf(event.relatedVideoId()));
        }
        return data;
    }

    /** 더 이상 유효하지 않은 토큰은 정리한다. 그대로 두면 매번 발송을 시도하게 된다. */
    private void handleSendFailure(PushDevice device, FirebaseMessagingException e) {
        if (isInvalidToken(e)) {
            log.info("만료/무효한 디바이스 토큰을 삭제한다. deviceId={}, errorCode={}",
                    device.getId(), e.getMessagingErrorCode());
            pushDeviceRepository.delete(device);
            return;
        }
        log.error("푸시 발송 실패. deviceId={}, errorCode={}", device.getId(), e.getMessagingErrorCode(), e);
    }

    /**
     * 토큰이 더 이상 유효하지 않아 삭제해야 하는 실패인지 판단한다.
     *
     * <p>FCM 오류 응답 본문을 SDK가 파싱하지 못하면 errorCode가 null로 남는다(실제로 관측됨).
     * 이때는 HTTP 상태로 판단한다. 단건 발송에서 400/404는 대상 토큰 문제인 경우가 대부분이다.</p>
     */
    private boolean isInvalidToken(FirebaseMessagingException e) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();
        if (errorCode != null) {
            return errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT;
        }
        IncomingHttpResponse response = e.getHttpResponse();
        return response != null && (response.getStatusCode() == 400 || response.getStatusCode() == 404);
    }

    private FirebaseMessaging initializeMessaging(String credentialsBase64) {
        if (!StringUtils.hasText(credentialsBase64)) {
            log.warn("firebase.credentials-base64가 설정되지 않아 푸시 발송이 비활성화된다. 알림은 DB에만 저장된다.");
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(credentialsBase64.trim());
            GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));
            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(existing -> FIREBASE_APP_NAME.equals(existing.getName()))
                    .findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(
                            FirebaseOptions.builder().setCredentials(credentials).build(),
                            FIREBASE_APP_NAME));
            log.info("FCM 푸시 발송이 활성화되었다.");
            return FirebaseMessaging.getInstance(app);
        } catch (IllegalArgumentException | IOException e) {
            // 키가 잘못됐다고 서버 기동을 막지는 않는다. 푸시만 비활성화하고 나머지 기능은 정상 동작시킨다.
            log.error("Firebase 자격 증명 초기화 실패. 푸시 발송이 비활성화된다. "
                    + "base64로 인코딩된 서비스 계정 키(JSON)가 맞는지 확인이 필요하다.", e);
            return null;
        }
    }
}
