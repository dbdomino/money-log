package com.dbdomino.moneylog.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

/**
 * 지출유형 — {@code tbl_user_expend_group}.
 *
 * <p>회원마다 자기 유형을 갖는다. 가입 시 10종(식비·교통·주거·통신·쇼핑·장보기·
 * 의료·교육·문화·기타)이 {@code defaultGroup = true}로 함께 생성된다(FR-020).
 *
 * <p><b>삭제해도 행을 지우지 않는다</b>(FR-037). 수단과 같은 방식이다. 삭제 표시된
 * 뒤에도 그 유형을 가리키던 지출·고정지출·목표금액·통계가 유형 정보를 계속 읽을 수
 * 있어야 한다. 덕분에 목표금액이 삭제된 유형을 참조해도 무방하다(FR-038).
 *
 * <p>삭제가 허용되는 조건 — 지출 내역이 한 번도 참조하지 않았고 시스템 기본 유형이
 * 아닐 것 — 은 <b>애플리케이션이 판정한다</b>(2.12의 {@code 3106}·{@code 3107}).
 * 삭제가 DELETE가 아니라 UPDATE라서 FK RESTRICT가 대신 막아줄 수 없기 때문이다.
 *
 * <p>이름 유일성은 <b>삭제 표시된 행까지 포함</b>한다. FR-035가 조건 없이 "같은 회원
 * 안에서 유일"이라고 정했고, 아이콘 파일명이 {@code {user_id}_{유형이름}.{확장자}}라
 * 이름을 재사용하면 삭제된 유형의 아이콘 파일을 덮어쓰기 때문이다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §5</a>
 */
@Entity
@Table(
        name = "tbl_user_expend_group",
        comment = "지출유형. 가입 시 기본 10종이 생성되며, 삭제해도 행은 남는다(삭제 표시)",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_user_expend_group_name",
                columnNames = {"id_key", "name"}
        ),
        indexes = @Index(
                name = "ix_user_expend_group_active",
                columnList = "id_key, in_use, deleted"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class UserExpendGroup extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    /** 소유 회원. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_key",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_expend_group_user")
    )
    private User user;

    /** 유형 이름. 같은 회원 안에서 유일하다(삭제 표시된 행 포함). */
    @Column(name = "name", nullable = false, length = 30)
    private String name;

    /** 사용 여부. 지출 입력 화면과 목표금액 목록에 띄울지 여부다. */
    @Column(name = "in_use", nullable = false)
    @ColumnDefault("true")
    private Boolean inUse = true;

    /** 시스템 기본 유형 여부. 가입 시 생성되는 10종이 {@code true}이며 삭제할 수 없다. */
    @Column(name = "default_group", nullable = false)
    @ColumnDefault("false")
    private Boolean defaultGroup = false;

    /**
     * 아이콘 파일명 (예: {@code user01_식비.png}). 아이콘이 없으면 비어 있다.
     *
     * <p>파일명만 담는다. 조회 경로({@code /api/v1/expend-groups/icons/{filename}})는
     * 응답을 만들 때 앞에 붙인다 — 경로를 저장하면 Base URL이 바뀔 때 전 행을 고쳐야
     * 한다(FR-036).
     *
     * <p>생성 시점에 정해지고 <b>유형 이름 변경을 따라가지 않는다</b>. 이름에서 매번
     * 다시 계산하면 이름을 바꾼 뒤 기존 아이콘 파일을 찾지 못한다.
     */
    @Column(name = "icon_filename", length = 255)
    private String iconFilename;

    /** 삭제 표시. 삭제해도 행은 남고 아이콘 파일도 남는다. */
    @Column(name = "deleted", nullable = false)
    @ColumnDefault("false")
    private Boolean deleted = false;
}
