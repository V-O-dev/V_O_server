package com.example.v_o_server.domain.notification.repository;

import com.example.v_o_server.domain.notification.entity.AppNotification;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

    /**
     * 커서(마지막으로 본 notificationId) 이전의 알림을 최신순으로 조회한다.
     * 댓글 목록과 달리 최신순이므로 커서보다 작은 id를 내림차순으로 가져온다.
     *
     * <p>content 조립에 actor 닉네임과 그룹명이 필요해 연관 엔티티를 함께 가져온다.
     * actor/group은 nullable이므로 left join.</p>
     */
    @Query("""
            select n from AppNotification n
            left join fetch n.actor
            left join fetch n.group
            left join fetch n.video
            where n.recipient.id = :userId
              and n.id < :cursorId
            order by n.id desc
            """)
    List<AppNotification> findByRecipientBeforeCursor(@Param("userId") Long userId,
                                                      @Param("cursorId") Long cursorId,
                                                      Pageable pageable);
}
