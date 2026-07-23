package com.example.v_o_server.domain.group.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.v_o_server.domain.archive.entity.ArchiveEntry;
import java.util.List;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 엔티티 매핑이 ERD 정본(`docs/ERD.md`)의 컬럼 길이·NULL 여부와 일치하는지 고정한다.
 *
 * <p>{@code ddl-auto=update}가 실제로 참조하는 Hibernate 매핑 모델을 직접 검사하므로
 * DB 연결이 필요 없다. ERD가 바뀌면 이 테스트도 함께 갱신해야 한다.</p>
 */
@DisplayName("ERD 컬럼 정합성")
class ErdColumnAlignmentTest {

    private static Metadata metadata;

    @BeforeAll
    static void buildMetadata() {
        MetadataSources sources = new MetadataSources(new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                // DB 없이 매핑 모델만 구축한다.
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                .build());
        List.of(
                com.example.v_o_server.domain.user.entity.User.class,
                PrivateGroup.class, GroupMember.class, GroupTheme.class, GroupInvite.class,
                com.example.v_o_server.domain.question.entity.Question.class,
                com.example.v_o_server.domain.question.entity.GroupDailyQuestion.class,
                com.example.v_o_server.domain.answer.entity.DailyAnswer.class,
                com.example.v_o_server.domain.answer.entity.Video.class,
                ArchiveEntry.class
        ).forEach(sources::addAnnotatedClass);
        metadata = sources.buildMetadata();
    }

    private static Column column(Class<?> entity, String property) {
        PersistentClass binding = metadata.getEntityBinding(entity.getName());
        Selectable selectable = binding.getProperty(property).getSelectables().get(0);
        return (Column) selectable;
    }

    @ParameterizedTest(name = "{0}.{1} -> VARCHAR({2})")
    @DisplayName("ERD의 VARCHAR 길이와 일치한다")
    @CsvSource({
            // private_groups
            "PrivateGroup, name, 15",
            "PrivateGroup, groupImageUrl, 1000",
            "PrivateGroup, groupImageObjectKey, 500",
            "PrivateGroup, timezone, 60",
            "PrivateGroup, status, 30",
            // group_members
            "GroupMember, role, 20",
            "GroupMember, status, 30",
            // group_themes
            "GroupTheme, code, 40",
            "GroupTheme, name, 80",
            "GroupTheme, description, 500",
            // group_invites
            "GroupInvite, inviteCode, 20",
            "GroupInvite, inviteUrl, 1000",
            "GroupInvite, qrImageUrl, 1000",
            "GroupInvite, status, 30",
            // archive_entries
            "ArchiveEntry, questionContentSnapshot, 500",
            "ArchiveEntry, groupNameSnapshot, 50",
            "ArchiveEntry, groupThemeSnapshot, 80"
    })
    void columnLengthMatchesErd(String entityName, String property, int expectedLength) {
        assertThat(column(entityFor(entityName), property).getLength())
                .as("%s.%s 길이", entityName, property)
                .isEqualTo(expectedLength);
    }

    @ParameterizedTest(name = "{0}.{1} nullable={2}")
    @DisplayName("ERD의 NOT NULL 제약과 일치한다")
    @CsvSource({
            "PrivateGroup, name, false",
            "PrivateGroup, notificationStartTime, false",
            "PrivateGroup, notificationEndTime, false",
            "PrivateGroup, timezone, false",
            "PrivateGroup, maxMembers, false",
            "PrivateGroup, status, false",
            "PrivateGroup, groupImageUrl, true",
            "PrivateGroup, deletedAt, true",
            "GroupMember, role, false",
            "GroupMember, status, false",
            "GroupMember, joinedAt, false",
            "GroupMember, leftAt, true",
            "GroupMember, kickedBy, true",
            "GroupTheme, code, false",
            "GroupTheme, name, false",
            "GroupTheme, sortOrder, false",
            "GroupTheme, isActive, false",
            "GroupInvite, inviteCode, false",
            "GroupInvite, usedCount, false",
            "GroupInvite, expiresAt, false",
            "GroupInvite, status, false",
            "ArchiveEntry, recordDate, false",
            "ArchiveEntry, questionContentSnapshot, false",
            "ArchiveEntry, groupNameSnapshot, false",
            "ArchiveEntry, groupThemeSnapshot, true"
    })
    void columnNullabilityMatchesErd(String entityName, String property, boolean expectedNullable) {
        assertThat(column(entityFor(entityName), property).isNullable())
                .as("%s.%s NULL 허용", entityName, property)
                .isEqualTo(expectedNullable);
    }

    /**
     * FK(조인 컬럼)의 NULL 여부도 ERD를 따른다.
     *
     * <p><b>예외 — {@code private_groups.owner_user_id}</b>: ERD는 NULL 허용이지만 엔티티는
     * {@code nullable=false}로 더 엄격하다. 현재 로직상 방장 없는 그룹은 만들어질 수 없어
     * (위임 필수, 마지막 멤버면 그룹 소프트 삭제) 의도적으로 더 강한 제약을 유지한다.
     * 유저 탈퇴 기능 도입 시 재검토 대상이므로 여기서는 단언하지 않는다.</p>
     */
    @ParameterizedTest(name = "{0}.{1}(FK) nullable={2}")
    @DisplayName("ERD의 FK NOT NULL 제약과 일치한다")
    @CsvSource({
            "PrivateGroup, theme, false",
            "GroupMember, group, false",
            "GroupMember, user, false",
            "GroupInvite, group, false",
            "GroupInvite, createdBy, false",
            "ArchiveEntry, user, false",
            "ArchiveEntry, group, false",
            "ArchiveEntry, groupDailyQuestion, false",
            "ArchiveEntry, dailyAnswer, false",
            "ArchiveEntry, video, false",
            "ArchiveEntry, question, false"
    })
    void joinColumnNullabilityMatchesErd(String entityName, String property, boolean expectedNullable) {
        assertThat(column(entityFor(entityName), property).isNullable())
                .as("%s.%s(FK) NULL 허용", entityName, property)
                .isEqualTo(expectedNullable);
    }

    @ParameterizedTest(name = "{0}.{1} 은 UNIQUE")
    @DisplayName("ERD/계획이 요구하는 UNIQUE 제약이 걸려 있다")
    @CsvSource({
            "GroupTheme, code",
            "GroupInvite, inviteCode"
    })
    void uniqueConstraintsExist(String entityName, String property) {
        assertThat(column(entityFor(entityName), property).isUnique())
                .as("%s.%s UNIQUE", entityName, property)
                .isTrue();
    }

    private static Class<?> entityFor(String simpleName) {
        return switch (simpleName) {
            case "PrivateGroup" -> PrivateGroup.class;
            case "GroupMember" -> GroupMember.class;
            case "GroupTheme" -> GroupTheme.class;
            case "GroupInvite" -> GroupInvite.class;
            case "ArchiveEntry" -> ArchiveEntry.class;
            default -> throw new IllegalArgumentException("알 수 없는 엔티티: " + simpleName);
        };
    }
}
