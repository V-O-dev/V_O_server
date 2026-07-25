package com.example.v_o_server.domain.group;

import com.example.v_o_server.domain.group.entity.GroupTheme;
import com.example.v_o_server.domain.group.repository.GroupThemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테마 seed 한 건을 독립 트랜잭션으로 기록한다.
 *
 * <p>{@link GroupThemeSeeder}와 분리한 이유: {@code REQUIRES_NEW}는 프록시를 통해 호출해야
 * 적용되므로 self-injection 대신 별도 빈으로 둔다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupThemeSeedWriter {

    private final GroupThemeRepository groupThemeRepository;

    /**
     * 테마 한 건을 독립 트랜잭션으로 insert한다(이미 있으면 no-op).
     *
     * <p>UNIQUE 충돌 시 {@link DataIntegrityViolationException}을 그대로 던진다. PostgreSQL은
     * 제약 위반이 나면 트랜잭션 전체를 abort 상태로 만들기 때문에, 예외를 이 트랜잭션 <b>안에서</b>
     * 삼키면 커밋 시점에 다시 실패한다. 따라서 중복 무시는 트랜잭션 경계 <b>밖</b>인
     * {@link GroupThemeSeeder}에서 처리한다.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void seed(String code, String name, String description, int sortOrder) {
        if (groupThemeRepository.existsByCode(code)) {
            return;
        }
        groupThemeRepository.saveAndFlush(GroupTheme.builder()
                .code(code)
                .name(name)
                .description(description)
                .sortOrder(sortOrder)
                .isActive(true)
                .build());
        log.info("그룹 테마 seed 완료: {}", code);
    }
}
