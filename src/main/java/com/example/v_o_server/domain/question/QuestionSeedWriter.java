package com.example.v_o_server.domain.question;

import com.example.v_o_server.domain.group.entity.GroupTheme;
import com.example.v_o_server.domain.group.repository.GroupThemeRepository;
import com.example.v_o_server.domain.question.entity.Question;
import com.example.v_o_server.domain.question.entity.QuestionStatus;
import com.example.v_o_server.domain.question.entity.QuestionThemeMap;
import com.example.v_o_server.domain.question.entity.QuestionThemeMapId;
import com.example.v_o_server.domain.question.repository.QuestionRepository;
import com.example.v_o_server.domain.question.repository.QuestionThemeMapRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 질문 seed 한 건을 독립 트랜잭션으로 기록한다.
 *
 * <p>{@link QuestionSeeder}와 분리한 이유는 {@link GroupThemeSeedWriter}와 동일하다.
 * {@code REQUIRES_NEW}는 프록시를 통해 호출해야 적용되므로 별도 빈으로 둔다.</p>
 *
 * @see com.example.v_o_server.domain.group.GroupThemeSeedWriter
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionSeedWriter {

    private final QuestionRepository questionRepository;
    private final QuestionThemeMapRepository questionThemeMapRepository;
    private final GroupThemeRepository groupThemeRepository;

    /**
     * 질문 한 건과 테마 매핑을 독립 트랜잭션으로 기록한다(이미 있으면 no-op).
     *
     * <p>{@code group_themes.code}와 달리 {@code questions.content}에는 UNIQUE 제약이 없어
     * DB가 중복을 막아주지 못한다. 앱이 재기동될 때마다 seed가 다시 도는 구조이므로,
     * 여기서 content로 조회해 중복 insert를 직접 차단한다.</p>
     *
     * <p>같은 질문이 여러 테마에 속할 수 있어 질문은 재사용하고 매핑만 추가한다.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void seed(String content, String themeCode, int answerTimeLimitMs) {
        Optional<GroupTheme> theme = groupThemeRepository.findByCode(themeCode);
        if (theme.isEmpty()) {
            // 테마 seed가 먼저 돌지 않았거나 코드가 바뀐 경우. 나머지 질문은 계속 진행한다.
            log.warn("질문 seed 건너뜀 - 테마를 찾을 수 없습니다: {}", themeCode);
            return;
        }

        Question question = questionRepository.findByContent(content)
                .orElseGet(() -> questionRepository.saveAndFlush(Question.builder()
                        .content(content)
                        .status(QuestionStatus.ACTIVE)
                        .answerTimeLimitMs(answerTimeLimitMs)
                        .useCount(0)
                        .build()));

        Long themeId = theme.get().getId();
        if (questionThemeMapRepository.existsById(new QuestionThemeMapId(question.getId(), themeId))) {
            return;
        }
        questionThemeMapRepository.saveAndFlush(QuestionThemeMap.builder()
                .question(question)
                .theme(theme.get())
                .build());
    }
}
