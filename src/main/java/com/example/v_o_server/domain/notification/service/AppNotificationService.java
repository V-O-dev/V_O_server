package com.example.v_o_server.domain.notification.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.notification.dto.response.NotificationListResponse;
import com.example.v_o_server.domain.notification.dto.response.NotificationReadResponse;
import com.example.v_o_server.domain.notification.dto.response.NotificationResponse;
import com.example.v_o_server.domain.notification.entity.AppNotification;
import com.example.v_o_server.domain.notification.entity.NotificationType;
import com.example.v_o_server.domain.notification.repository.AppNotificationRepository;
import com.example.v_o_server.domain.question.entity.GroupDailyQuestion;
import com.example.v_o_server.domain.user.entity.User;
import com.example.v_o_server.domain.user.repository.UserProfileRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppNotificationService {

    private static final int PAGE_SIZE = 20;

    private static final String TITLE_COMMENT = "새 댓글";
    private static final String TITLE_REACTION = "새 좋아요";
    private static final String TITLE_DAILY_QUESTION = "오늘의 질문";

    private final AppNotificationRepository appNotificationRepository;
    private final UserProfileRepository userProfileRepository;

    /** 알림 목록 조회 (최신순). 알림이 없으면 빈 목록을 반환한다(에러 아님). */
    public NotificationListResponse getNotifications(Long userId, Long cursor) {
        Pageable limit = PageRequest.of(0, PAGE_SIZE + 1);
        // 최신순이라 커서는 "이 id보다 작은 것"을 의미한다. 첫 페이지는 상한 없음.
        long cursorId = cursor == null ? Long.MAX_VALUE : cursor;

        List<AppNotification> fetched =
                appNotificationRepository.findByRecipientBeforeCursor(userId, cursorId, limit);

        boolean hasNext = fetched.size() > PAGE_SIZE;
        List<AppNotification> notifications = hasNext ? fetched.subList(0, PAGE_SIZE) : fetched;

        return new NotificationListResponse(
                notifications.stream().map(NotificationResponse::from).toList(),
                hasNext
        );
    }

    @Transactional
    public NotificationReadResponse markAsRead(Long userId, Long notificationId) {
        AppNotification notification = appNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.isReceivedBy(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NO_PERMISSION);
        }

        notification.markAsRead(LocalDateTime.now());
        return NotificationReadResponse.from(notification);
    }

    /* -------------------- 알림 생성 (다른 도메인에서 호출) -------------------- */

    /**
     * 댓글 등록 시 영상 주인에게 알림을 생성한다.
     * 자기 영상에 자기가 댓글을 단 경우는 생성하지 않는다.
     */
    @Transactional
    public void createCommentNotification(Video video, User actor, String commentContent) {
        User recipient = video.getUser();
        if (isSelfAction(recipient, actor)) {
            return;
        }

        String body = "%s님이 댓글을 남겼습니다: \"%s\"".formatted(nicknameOf(actor), commentContent);
        save(NotificationType.COMMENT, TITLE_COMMENT, body, recipient, actor,
                video.getGroup(), groupDailyQuestionOf(video), video);
    }

    /**
     * 좋아요 등록 시 영상 주인에게 알림을 생성한다.
     * 자기 영상에 자기가 좋아요를 누른 경우는 생성하지 않는다.
     */
    @Transactional
    public void createReactionNotification(Video video, User actor) {
        User recipient = video.getUser();
        if (isSelfAction(recipient, actor)) {
            return;
        }

        String body = "%s님이 내 오늘 자 기록에 좋아요를 보냈습니다.".formatted(nicknameOf(actor));
        save(NotificationType.REACTION, TITLE_REACTION, body, recipient, actor,
                video.getGroup(), groupDailyQuestionOf(video), video);
    }

    /**
     * 오늘의 질문 배정 시 그룹원에게 알림을 생성한다.
     * 시스템이 발생시키는 알림이라 actor가 없고, 특정 영상과도 무관하다.
     */
    @Transactional
    public void createDailyQuestionNotification(GroupDailyQuestion groupDailyQuestion, User recipient) {
        PrivateGroup group = groupDailyQuestion.getGroup();
        String body = "오늘 새로운 %s의 질문이 수신되었습니다.".formatted(group.getName());
        save(NotificationType.DAILY_QUESTION, TITLE_DAILY_QUESTION, body, recipient, null,
                group, groupDailyQuestion, null);
    }

    private void save(NotificationType type, String title, String body, User recipient, User actor,
            PrivateGroup group, GroupDailyQuestion groupDailyQuestion, Video video) {
        // TODO: FCM 연동 시 여기서 푸시 발송을 트리거한다. 단 User의 알림 설정
        //  (interactionNotificationEnabled / dailyQuestionNotificationEnabled)이 꺼져 있으면
        //  DB 저장은 하되 푸시는 보내지 않는다.
        appNotificationRepository.save(AppNotification.builder()
                .notificationType(type)
                .title(title)
                .body(body)
                .isRead(false)
                .recipient(recipient)
                .actor(actor)
                .group(group)
                .groupDailyQuestion(groupDailyQuestion)
                .video(video)
                .build());
    }

    private boolean isSelfAction(User recipient, User actor) {
        return recipient.getId().equals(actor.getId());
    }

    /** 프로필이 아직 없는 사용자도 알림 생성이 실패하지 않도록 기본 문구로 대체한다. */
    private String nicknameOf(User user) {
        return userProfileRepository.findById(user.getId())
                .map(profile -> profile.getNickname())
                .orElse("알 수 없는 사용자");
    }

    private GroupDailyQuestion groupDailyQuestionOf(Video video) {
        return video.getDailyAnswer() == null ? null : video.getDailyAnswer().getGroupDailyQuestion();
    }
}
