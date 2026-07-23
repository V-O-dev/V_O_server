package com.example.v_o_server.domain.answer.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 좋아요 연타 어뷰징 방지. 사용자·영상 단위로 짧은 TTL 키를 걸어 반복 요청을 차단한다.
 */
@Component
@RequiredArgsConstructor
public class ReactionRateLimiter {

    private static final String KEY_PREFIX = "ratelimit:reaction:";
    private static final Duration WINDOW = Duration.ofSeconds(1);

    private final StringRedisTemplate redisTemplate;

    /** 허용되면 통과, 직전 요청 후 {@link #WINDOW} 안의 재요청이면 TOO_MANY_REQUESTS를 던진다. */
    public void checkRate(Long userId, Long videoId) {
        String key = KEY_PREFIX + userId + ":" + videoId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", WINDOW);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }
}
