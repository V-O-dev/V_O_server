package com.example.v_o_server.domain.question.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.v_o_server.common.exception.BusinessException;
import com.example.v_o_server.common.exception.ErrorCode;
import com.example.v_o_server.domain.group.entity.GroupStatus;
import com.example.v_o_server.domain.group.entity.GroupTheme;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QuestionServiceTest {

    private static final Long USER_ID = 7L;

    private GroupAccessGuard groupAccessGuard;
    private QuestionRepository questionRepository;
    private GroupDailyQuestionRepository groupDailyQuestionRepository;
    private QuestionService questionService;

    @BeforeEach
    void setUp() {
        groupAccessGuard = mock(GroupAccessGuard.class);
        questionRepository = mock(QuestionRepository.class);
        groupDailyQuestionRepository = mock(GroupDailyQuestionRepository.class);
        questionService = new QuestionService(groupAccessGuard, questionRepository, groupDailyQuestionRepository);
    }

    @Test
    void 오늘_이미_배정된_질문이_있으면_그대로_반환하고_재배정하지_않는다() {
        Long groupId = 1L;
        LocalDate today = LocalDate.now();
        Question question = questionWithId(10L, "질문A", 0);
        GroupDailyQuestion existing = dailyQuestionWithId(100L, question, today);
        given(groupAccessGuard.getActiveGroup(groupId))
                .willReturn(activeGroupWithTheme(groupId, null));

        given(groupDailyQuestionRepository.findByGroupIdAndServiceDate(groupId, today))
                .willReturn(Optional.of(existing));

        DailyQuestionResponse response = questionService.getDailyQuestion(USER_ID, groupId);

        assertThat(response.groupDailyQuestionId()).isEqualTo(100L);
        assertThat(response.questionId()).isEqualTo(10L);
        verify(groupAccessGuard).assertMember(groupId, USER_ID);
        verifyNoInteractions(questionRepository);
        verify(groupDailyQuestionRepository, never()).save(any());
    }

    @Test
    void 오늘_배정_이력이_없으면_테마에_맞는_질문_중에서_새로_배정한다() {
        Long groupId = 1L;
        GroupTheme theme = themeWithId(1L, "FAMILY");
        PrivateGroup group = activeGroupWithTheme(1L, theme);
        Question onlyCandidate = questionWithId(10L, "유일한 질문", 0);

        given(groupAccessGuard.getActiveGroup(groupId)).willReturn(group);
        given(groupDailyQuestionRepository.findByGroupIdAndServiceDate(eq(groupId), any()))
                .willReturn(Optional.empty());
        given(questionRepository.findByThemeIdAndStatus(1L, QuestionStatus.ACTIVE))
                .willReturn(List.of(onlyCandidate));
        given(groupDailyQuestionRepository.findLastShownDatesByGroupId(groupId))
                .willReturn(List.of());
        given(groupDailyQuestionRepository.save(any(GroupDailyQuestion.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        DailyQuestionResponse response = questionService.getDailyQuestion(USER_ID, groupId);

        assertThat(response.questionId()).isEqualTo(10L);
        assertThat(response.content()).isEqualTo("유일한 질문");
        assertThat(onlyCandidate.getUseCount()).isEqualTo(1);
        verify(groupAccessGuard).assertMember(groupId, USER_ID);
        verify(groupDailyQuestionRepository).save(any(GroupDailyQuestion.class));
    }

    @Test
    void 그룹이_없으면_GROUP_NOT_FOUND_예외를_던진다() {
        Long groupId = 999L;
        given(groupAccessGuard.getActiveGroup(groupId))
                .willThrow(new BusinessException(ErrorCode.GROUP_NOT_FOUND));

        BusinessException exception = catchThrowableOfType(
                () -> questionService.getDailyQuestion(USER_ID, groupId), BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GROUP_NOT_FOUND);
        verifyNoInteractions(groupDailyQuestionRepository, questionRepository);
    }

    @Test
    void 그룹_비멤버면_NOT_GROUP_MEMBER_예외를_던진다() {
        Long groupId = 1L;
        given(groupAccessGuard.getActiveGroup(groupId))
                .willReturn(activeGroupWithTheme(groupId, null));
        willThrow(new BusinessException(ErrorCode.NOT_GROUP_MEMBER))
                .given(groupAccessGuard)
                .assertMember(groupId, USER_ID);

        BusinessException exception = catchThrowableOfType(
                () -> questionService.getDailyQuestion(USER_ID, groupId), BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_GROUP_MEMBER);
        verifyNoInteractions(groupDailyQuestionRepository, questionRepository);
    }

    @Test
    void 그룹에_테마가_없으면_GROUP_THEME_NOT_SET_예외를_던진다() {
        Long groupId = 1L;
        PrivateGroup group = activeGroupWithTheme(1L, null);

        given(groupAccessGuard.getActiveGroup(groupId)).willReturn(group);
        given(groupDailyQuestionRepository.findByGroupIdAndServiceDate(eq(groupId), any()))
                .willReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> questionService.getDailyQuestion(USER_ID, groupId), BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GROUP_THEME_NOT_SET);
    }

    @Test
    void 테마에_매핑된_활성_질문이_없으면_NO_QUESTION_CANDIDATE_예외를_던진다() {
        Long groupId = 1L;
        GroupTheme theme = themeWithId(1L, "FAMILY");
        PrivateGroup group = activeGroupWithTheme(1L, theme);

        given(groupAccessGuard.getActiveGroup(groupId)).willReturn(group);
        given(groupDailyQuestionRepository.findByGroupIdAndServiceDate(eq(groupId), any()))
                .willReturn(Optional.empty());
        given(questionRepository.findByThemeIdAndStatus(1L, QuestionStatus.ACTIVE))
                .willReturn(List.of());

        BusinessException exception = catchThrowableOfType(
                () -> questionService.getDailyQuestion(USER_ID, groupId), BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_QUESTION_CANDIDATE);
    }

    @Test
    void 쿨타임_이내에_나온_질문은_후보에서_제외된다() {
        Long groupId = 1L;
        GroupTheme theme = themeWithId(1L, "FAMILY");
        PrivateGroup group = activeGroupWithTheme(1L, theme);
        Question recentlyShown = questionWithId(10L, "최근에 나온 질문", 5);
        Question eligible = questionWithId(11L, "제외 안 된 질문", 5);

        given(groupAccessGuard.getActiveGroup(groupId)).willReturn(group);
        given(groupDailyQuestionRepository.findByGroupIdAndServiceDate(eq(groupId), any()))
                .willReturn(Optional.empty());
        given(questionRepository.findByThemeIdAndStatus(1L, QuestionStatus.ACTIVE))
                .willReturn(List.of(recentlyShown, eligible));
        given(groupDailyQuestionRepository.findLastShownDatesByGroupId(groupId))
                .willReturn(List.of(new QuestionLastShownDate(10L, LocalDate.now().minusDays(5))));
        given(groupDailyQuestionRepository.save(any(GroupDailyQuestion.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        DailyQuestionResponse response = questionService.getDailyQuestion(USER_ID, groupId);

        assertThat(response.questionId()).isEqualTo(11L);
    }

    @Test
    void 모든_후보가_쿨타임_이내면_가장_오래전에_나온_질문을_선택한다() {
        Long groupId = 1L;
        GroupTheme theme = themeWithId(1L, "FAMILY");
        PrivateGroup group = activeGroupWithTheme(1L, theme);
        Question shownRecently = questionWithId(10L, "최근", 3);
        Question shownLongerAgo = questionWithId(11L, "더 오래된", 3);

        given(groupAccessGuard.getActiveGroup(groupId)).willReturn(group);
        given(groupDailyQuestionRepository.findByGroupIdAndServiceDate(eq(groupId), any()))
                .willReturn(Optional.empty());
        given(questionRepository.findByThemeIdAndStatus(1L, QuestionStatus.ACTIVE))
                .willReturn(List.of(shownRecently, shownLongerAgo));
        given(groupDailyQuestionRepository.findLastShownDatesByGroupId(groupId))
                .willReturn(List.of(
                        new QuestionLastShownDate(10L, LocalDate.now().minusDays(2)),
                        new QuestionLastShownDate(11L, LocalDate.now().minusDays(10))
                ));
        given(groupDailyQuestionRepository.save(any(GroupDailyQuestion.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        DailyQuestionResponse response = questionService.getDailyQuestion(USER_ID, groupId);

        assertThat(response.questionId()).isEqualTo(11L);
    }

    private Question questionWithId(Long id, String content, int useCount) {
        Question question = Question.builder()
                .content(content)
                .status(QuestionStatus.ACTIVE)
                .answerTimeLimitMs(10000)
                .useCount(useCount)
                .build();
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private GroupTheme themeWithId(Long id, String code) {
        GroupTheme theme = GroupTheme.builder()
                .code(code)
                .name(code)
                .sortOrder(1)
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(theme, "id", id);
        return theme;
    }

    private PrivateGroup activeGroupWithTheme(Long id, GroupTheme theme) {
        PrivateGroup group = PrivateGroup.builder()
                .theme(theme)
                .name("테스트그룹")
                .status(GroupStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    private GroupDailyQuestion dailyQuestionWithId(Long id, Question question, LocalDate serviceDate) {
        GroupDailyQuestion dailyQuestion = GroupDailyQuestion.builder()
                .question(question)
                .serviceDate(serviceDate)
                .questionContentSnapshot(question.getContent())
                .answerTimeLimitMs(question.getAnswerTimeLimitMs())
                .assignedAt(LocalDateTime.now())
                .expiresAt(serviceDate.plusDays(1).atStartOfDay())
                .status(AssignmentStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(dailyQuestion, "id", id);
        return dailyQuestion;
    }
}
