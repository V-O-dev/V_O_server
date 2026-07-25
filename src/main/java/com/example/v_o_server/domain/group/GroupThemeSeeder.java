package com.example.v_o_server.domain.group;

import com.example.v_o_server.domain.group.entity.GroupThemeCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * 기본 그룹 테마(FRIEND/COUPLE/FAMILY/RANDOM) seed.
 *
 * <p>{@code group_themes.code}에 DB UNIQUE 제약이 있어, 다중 인스턴스가 동시에 기동해도
 * 중복 insert는 DB가 차단한다. 실제 기록은 {@link GroupThemeSeedWriter}가 테마별 독립
 * 트랜잭션으로 수행한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupThemeSeeder implements ApplicationRunner {

    private record ThemeSeed(GroupThemeCode code, String name, String description, int sortOrder) {
    }

    private static final List<ThemeSeed> SEEDS = List.of(
            new ThemeSeed(GroupThemeCode.FRIEND, "친구", "친구들과 함께하는 그룹", 1),
            new ThemeSeed(GroupThemeCode.COUPLE, "연인", "연인과 함께하는 그룹", 2),
            new ThemeSeed(GroupThemeCode.FAMILY, "가족", "가족과 함께하는 그룹", 3),
            new ThemeSeed(GroupThemeCode.RANDOM, "랜덤", "다양한 주제의 질문을 받는 그룹", 4)
    );

    private final GroupThemeSeedWriter seedWriter;

    @Override
    public void run(ApplicationArguments args) {
        for (ThemeSeed seed : SEEDS) {
            String code = seed.code().name();
            try {
                seedWriter.seed(code, seed.name(), seed.description(), seed.sortOrder());
            } catch (DataIntegrityViolationException e) {
                // 다른 인스턴스가 먼저 넣은 경우. 해당 테마의 트랜잭션만 롤백되므로 나머지 seed는 계속 진행한다.
                log.debug("그룹 테마 seed 중복 - 무시합니다: {}", code);
            }
        }
    }
}
