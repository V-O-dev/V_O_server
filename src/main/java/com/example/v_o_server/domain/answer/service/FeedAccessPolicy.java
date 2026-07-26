package com.example.v_o_server.domain.answer.service;

import com.example.v_o_server.domain.answer.entity.Video;

/**
 * "오늘의 답변(피드)" 완료 여부에 따른 접근 제어(FEED_LOCKED) 정책.
 * 미완료 사용자는 다른 멤버 영상의 리액션/댓글을 조회·작성할 수 없다.
 */
public interface FeedAccessPolicy {

    /** 사용자가 해당 영상의 리액션/댓글에 접근할 수 있는지 판단한다. */
    boolean canAccess(Long userId, Video video);
}
