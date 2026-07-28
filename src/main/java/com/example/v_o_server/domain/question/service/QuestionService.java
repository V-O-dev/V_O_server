package com.example.v_o_server.domain.question.service;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.group.service.GroupAccessGuard;
import com.example.v_o_server.domain.question.dto.response.DailyQuestionResponse;
import com.example.v_o_server.domain.question.entity.AssignmentStatus;
import com.example.v_o_server.domain.question.entity.GroupDailyQuestion;
import com.example.v_o_server.domain.question.entity.Question;
import com.example.v_o_server.domain.question.entity.QuestionStatus;
import com.example.v_o_server.domain.question.repository.GroupDailyQuestionRepository;
import com.example.v_o_server.domain.question.repository.QuestionLastShownDate;
import com.example.v_o_server.domain.question.repository.QuestionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionService {

    /** 같은 그룹에는 이 기간(일) 내에 나왔던 질문을 우선적으로 다시 배정하지 않는다. */
    private static final int COOLDOWN_DAYS = 30;

    private final GroupAccessGuard groupAccessGuard;
    private final QuestionRepository questionRepository;
    private final GroupDailyQuestionRepository groupDailyQuestionRepository;

    /**
     * 그룹의 오늘의 질문을 조회한다. 아직 배정된 적이 없으면 이 시점에 새로 배정한다.
     */
    @Transactional
    public DailyQuestionResponse getDailyQuestion(Long userId, Long groupId) {
        PrivateGroup group = groupAccessGuard.getActiveGroup(groupId);
        groupAccessGuard.assertMember(groupId, userId);

        LocalDate today = LocalDate.now();

        GroupDailyQuestion dailyQuestion = groupDailyQuestionRepository
                .findByGroupIdAndServiceDate(groupId, today)
                .orElseGet(() -> assignDailyQuestion(group, today));

        return toResponse(dailyQuestion);
    }

    private GroupDailyQuestion assignDailyQuestion(PrivateGroup group, LocalDate today) {
        if (group.getTheme() == null) {
            throw new BusinessException(ErrorCode.GROUP_THEME_NOT_SET);
        }

        Long groupId = group.getId();
        List<Question> candidates = questionRepository.findByThemeIdAndStatus(
                group.getTheme().getId(), QuestionStatus.ACTIVE);
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_QUESTION_CANDIDATE);
        }

        Map<Long, LocalDate> lastShownByQuestionId = groupDailyQuestionRepository
                .findLastShownDatesByGroupId(groupId).stream()
                .collect(Collectors.toMap(QuestionLastShownDate::questionId, QuestionLastShownDate::lastServiceDate));

        LocalDate cooldownCutoff = today.minusDays(COOLDOWN_DAYS);
        List<Question> eligibleCandidates = candidates.stream()
                .filter(q -> {
                    LocalDate lastShown = lastShownByQuestionId.get(q.getId());
                    return lastShown == null || lastShown.isBefore(cooldownCutoff);
                })
                .toList();

        Question selected = eligibleCandidates.isEmpty()
                ? pickOldestShown(candidates, lastShownByQuestionId)
                : pickByUsageWeightedRandom(eligibleCandidates);
        selected.use(LocalDateTime.now());

        GroupDailyQuestion dailyQuestion = GroupDailyQuestion.builder()
                .group(group)
                .question(selected)
                .serviceDate(today)
                .questionContentSnapshot(selected.getContent())
                .answerTimeLimitMs(selected.getAnswerTimeLimitMs())
                .assignedAt(LocalDateTime.now())
                .expiresAt(today.plusDays(1).atStartOfDay())
                .status(AssignmentStatus.ACTIVE)
                .build();

        return groupDailyQuestionRepository.save(dailyQuestion);
    }

    /**
     * 적게 사용된 질문일수록(use_count가 낮을수록) 더 잘 뽑히도록 가중 랜덤으로 선택한다.
     * 가중치 = 1 / (useCount + 1)
     */
    private Question pickByUsageWeightedRandom(List<Question> candidates) {
        double totalWeight = candidates.stream()
                .mapToDouble(q -> 1.0 / (q.getUseCount() + 1))
                .sum();

        double point = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double cumulative = 0;
        for (Question candidate : candidates) {
            cumulative += 1.0 / (candidate.getUseCount() + 1);
            if (point <= cumulative) {
                return candidate;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    /**
     * 쿨타임 내에 안 나온 질문이 하나도 없을 때, 이 그룹에 가장 오래전에 나왔던(마지막 노출일이 가장 이른) 질문을 선택한다.
     */
    private Question pickOldestShown(List<Question> candidates, Map<Long, LocalDate> lastShownByQuestionId) {
        return candidates.stream()
                .min(Comparator.comparing(q -> lastShownByQuestionId.get(q.getId())))
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_QUESTION_CANDIDATE));
    }

    private DailyQuestionResponse toResponse(GroupDailyQuestion dailyQuestion) {
        return new DailyQuestionResponse(
                dailyQuestion.getId(),
                dailyQuestion.getQuestion().getId(),
                dailyQuestion.getQuestionContentSnapshot(),
                dailyQuestion.getAnswerTimeLimitMs(),
                dailyQuestion.getServiceDate(),
                dailyQuestion.getExpiresAt()
        );
    }
}
