package com.example.v_o_server.domain.group.entity;

import com.example.v_o_server.common.entity.BaseTimeEntity;
import com.example.v_o_server.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 그룹 안에서 "나만 보는" 멤버 호칭.
 *
 * <p>같은 그룹의 같은 대상이라도 보는 사람마다 다른 호칭을 가질 수 있으므로
 * 키가 {@code (그룹, 보는 사람, 대상)} 3중이다. {@code group_members}는 {@code (그룹, 대상)} 2중이라
 * 이 관계를 담을 수 없어 별도 테이블로 분리했다.</p>
 *
 * <p>호칭은 설정한 사람({@code viewer})에게만 노출된다. 대상이나 다른 멤버는 알 수 없다.</p>
 */
@Getter
@Entity
@Table(name = "group_member_aliases",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_group_member_aliases_group_viewer_target",
                columnNames = {"group_id", "viewer_user_id", "target_user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMemberAlias extends BaseTimeEntity {

    /** 전역 닉네임과 동일한 상한. 화면 문구도 닉네임과 같은 규칙을 쓴다. */
    public static final int MAX_ALIAS_LENGTH = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private PrivateGroup group;

    /** 호칭을 설정한 사람. 이 호칭은 오직 이 사용자에게만 보인다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_user_id", nullable = false)
    private User viewer;

    /** 호칭이 붙는 대상. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User target;

    @Column(name = "alias", nullable = false, length = MAX_ALIAS_LENGTH)
    private String alias;

    @Builder
    private GroupMemberAlias(PrivateGroup group, User viewer, User target, String alias) {
        this.group = group;
        this.viewer = viewer;
        this.target = target;
        this.alias = alias;
    }

    public void updateAlias(String alias) {
        this.alias = alias;
    }
}
