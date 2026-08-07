package com.example.v_o_server.domain.notification.event;

import com.example.v_o_server.domain.notification.entity.NotificationType;

/**
 * 알림이 저장된 뒤 푸시 발송을 트리거하는 이벤트.
 *
 * <p>수신자의 알림 수신 설정이 켜져 있을 때만 발행된다(설정이 꺼져 있어도 DB 저장은 이뤄진다).
 * 커밋 이후에 처리되므로 지연 로딩이 불가능하다. 따라서 엔티티가 아닌 식별자·문자열만 담는다.</p>
 */
public record NotificationCreatedEvent(
        Long recipientId,
        NotificationType type,
        String title,
        String body,
        Long relatedVideoId
) {
}
