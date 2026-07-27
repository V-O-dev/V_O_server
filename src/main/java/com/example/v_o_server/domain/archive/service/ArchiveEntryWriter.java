package com.example.v_o_server.domain.archive.service;

import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.archive.entity.ArchiveEntry;
import com.example.v_o_server.domain.archive.repository.ArchiveEntryRepository;
import com.example.v_o_server.domain.group.entity.GroupTheme;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.question.entity.GroupDailyQuestion;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 나의 달력(아카이브) 기록 생성 전담.
 *
 * <p>영상 업로드 시점에 호출되며, 호출자(업로드)의 트랜잭션에 참여한다(전파 REQUIRED 기본).
 * 즉 업로드가 롤백되면 이 기록도 함께 롤백되어 반쪽 데이터가 남지 않는다.</p>
 *
 * <p>카드에 표시할 값들은 그 시점의 스냅샷으로 굳혀 저장한다. 이후 그룹명·질문 문구가 바뀌어도
 * 과거 기록 카드는 당시 모습 그대로 유지된다.</p>
 */
@Component
@RequiredArgsConstructor
public class ArchiveEntryWriter {

    private final ArchiveEntryRepository archiveEntryRepository;

    /**
     * 업로드된 영상으로부터 달력 기록 한 건을 생성한다.
     *
     * @param video             방금 저장된 영상 (그룹·사용자·질문·답변 참조를 여기서 얻는다)
     * @param groupDailyQuestion 그 날 배정된 질문 (질문 문구 스냅샷의 출처)
     * @param recordDate        달력 Dot 기준 날짜 (업로드일 = 서비스일)
     */
    public ArchiveEntry record(Video video, GroupDailyQuestion groupDailyQuestion, LocalDate recordDate) {
        PrivateGroup group = video.getGroup();
        GroupTheme theme = group.getTheme();
        return archiveEntryRepository.save(ArchiveEntry.builder()
                .user(video.getUser())
                .group(group)
                .groupDailyQuestion(groupDailyQuestion)
                .dailyAnswer(video.getDailyAnswer())
                .video(video)
                .question(video.getQuestion())
                .recordDate(recordDate)
                .questionContentSnapshot(groupDailyQuestion.getQuestionContentSnapshot())
                .groupNameSnapshot(group.getName())
                .groupThemeSnapshot(theme != null ? theme.getCode() : null)
                .build());
    }
}
