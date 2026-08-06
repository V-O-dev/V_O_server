package com.example.v_o_server.domain.archive.service;

import static com.example.v_o_server.domain.group.GroupTestFixtures.group;
import static com.example.v_o_server.domain.group.GroupTestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.v_o_server.domain.answer.entity.DailyAnswer;
import com.example.v_o_server.domain.answer.entity.Video;
import com.example.v_o_server.domain.archive.entity.ArchiveEntry;
import com.example.v_o_server.domain.archive.repository.ArchiveEntryRepository;
import com.example.v_o_server.domain.group.entity.PrivateGroup;
import com.example.v_o_server.domain.question.entity.GroupDailyQuestion;
import com.example.v_o_server.domain.question.entity.Question;
import com.example.v_o_server.domain.user.entity.User;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArchiveEntryWriter")
class ArchiveEntryWriterTest {

    private static final LocalDate DATE = LocalDate.of(2026, 7, 9);

    @Mock
    private ArchiveEntryRepository archiveEntryRepository;

    @InjectMocks
    private ArchiveEntryWriter archiveEntryWriter;

    private Video videoWith(PrivateGroup group) {
        User user = user(1L);
        DailyAnswer dailyAnswer = mock(DailyAnswer.class);
        Question question = mock(Question.class);
        Video video = mock(Video.class);
        given(video.getGroup()).willReturn(group);
        given(video.getUser()).willReturn(user);
        given(video.getDailyAnswer()).willReturn(dailyAnswer);
        given(video.getQuestion()).willReturn(question);
        return video;
    }

    @Test
    @DisplayName("업로드 영상으로 그 시점 스냅샷을 굳혀 기록을 저장한다")
    void recordsSnapshotFromUpload() {
        // group 픽스처는 theme code = FAMILY, name = "테스트 그룹"
        PrivateGroup group = group(100L, user(1L), 15);
        Video video = videoWith(group);
        GroupDailyQuestion gdq = mock(GroupDailyQuestion.class);
        given(gdq.getQuestionContentSnapshot()).willReturn("오늘 가장 기뻤던 순간은?");
        given(archiveEntryRepository.save(any(ArchiveEntry.class)))
                .willAnswer(inv -> inv.getArgument(0));

        archiveEntryWriter.record(video, gdq, DATE);

        ArgumentCaptor<ArchiveEntry> captor = ArgumentCaptor.forClass(ArchiveEntry.class);
        org.mockito.Mockito.verify(archiveEntryRepository).save(captor.capture());
        ArchiveEntry saved = captor.getValue();
        assertThat(saved.getRecordDate()).isEqualTo(DATE);
        assertThat(saved.getQuestionContentSnapshot()).isEqualTo("오늘 가장 기뻤던 순간은?");
        assertThat(saved.getGroupNameSnapshot()).isEqualTo("테스트 그룹");
        assertThat(saved.getGroupThemeSnapshot()).isEqualTo("FAMILY");
        assertThat(saved.getGroup()).isSameAs(group);
        assertThat(saved.getVideo()).isSameAs(video);
        assertThat(saved.getGroupDailyQuestion()).isSameAs(gdq);
    }

    @Test
    @DisplayName("테마가 없으면 groupThemeSnapshot은 null이다")
    void nullThemeYieldsNullSnapshot() {
        PrivateGroup group = mock(PrivateGroup.class);
        given(group.getTheme()).willReturn(null);
        given(group.getName()).willReturn("무테마 그룹");
        Video video = videoWith(group);
        GroupDailyQuestion gdq = mock(GroupDailyQuestion.class);
        given(gdq.getQuestionContentSnapshot()).willReturn("질문");
        given(archiveEntryRepository.save(any(ArchiveEntry.class)))
                .willAnswer(inv -> inv.getArgument(0));

        ArchiveEntry saved = archiveEntryWriter.record(video, gdq, DATE);

        assertThat(saved.getGroupThemeSnapshot()).isNull();
        assertThat(saved.getGroupNameSnapshot()).isEqualTo("무테마 그룹");
    }
}
