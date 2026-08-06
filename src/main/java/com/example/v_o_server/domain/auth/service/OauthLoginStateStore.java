package com.example.v_o_server.domain.auth.service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * OAuth 로그인 시작 시점의 정보를 state 값에 묶어 보관한다.
 *
 * <p>provider는 authorize 요청의 state를 콜백에 그대로 돌려주므로, 이를 열쇠 삼아
 * "로그인을 시작한 프론트가 어디였는지"를 콜백에서 복원한다. 덕분에 서버 설정 하나로
 * 로컬(5173)과 Vercel을 동시에 지원할 수 있다.</p>
 *
 * <p>state를 URL에 직접 인코딩하지 않고 서버에 보관하는 이유는 위변조를 막고
 * 1회용으로 소비(CSRF 방지)하기 위해서다.</p>
 */
@Component
@RequiredArgsConstructor
public class OauthLoginStateStore {

    private static final String KEY_PREFIX = "auth:oauth-state:";
    /** 사용자가 provider 로그인 화면에 머무는 시간을 감안한 여유값. */
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    /** 프론트 콜백 주소를 저장하고 provider에 넘길 state를 발급한다. */
    public String issue(String frontendRedirectUri) {
        String state = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + state, frontendRedirectUri, TTL);
        return state;
    }

    /** state에 묶인 프론트 콜백 주소를 꺼내고 즉시 폐기한다(1회용). */
    public Optional<String> consume(String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }
        String key = KEY_PREFIX + state;
        String frontendRedirectUri = redisTemplate.opsForValue().get(key);
        if (frontendRedirectUri != null) {
            redisTemplate.delete(key);
        }
        return Optional.ofNullable(frontendRedirectUri);
    }
}
