package com.dbdomino.moneylog.data.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

/**
 * 회원 — {@code tbl_user}.
 *
 * <p>기본키가 {@code idx}가 아니라 <b>{@code id_key}</b>인 유일한 저장 단위다.
 * 나머지 14개 테이블은 소유자 항목으로 이 {@code id_key}를 참조하며, 로그인
 * 아이디({@code user_id})를 복사해 두지 않는다.
 *
 * <p>API가 주고받는 {@code memberId}는 여기 {@code user_id} 값이다. 인증 필터가
 * 토큰의 {@code sub}를 {@code id_key}로 환산해 넘기는 것을 전제한다.
 *
 * <p>이메일 유일성은 "값이 있을 때만"이라 <b>부분</b> 유니크 인덱스가 필요한데,
 * {@code @Table(uniqueConstraints = ...)}에는 {@code WHERE} 절을 붙일 자리가 없다.
 * 그래서 {@code ux_user_email}만 {@link com.dbdomino.moneylog.data.config.MoneylogSchemaContributor}
 * 가 만든다. {@code role} 값 제한({@code 1} 또는 {@code 3})은 아래 {@code @Table} 의
 * {@code ck_user_role} 이 맡는다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §1</a>
 */
@Entity
@Table(
        name = "tbl_user",
        comment = "회원. 로그인 계정과 프로필. 나머지 14개 테이블이 id_key 로 이 행을 참조한다",
        uniqueConstraints = @UniqueConstraint(name = "ux_user_user_id", columnNames = "user_id"),
        check = @CheckConstraint(name = "ck_user_role", constraint = "role in (1, 3)")
)
// 회원가입은 자기 자신을 만드는 행위라 INSERT 시점에 id_key가 없다. created_by 뿐
// 아니라 updated_by도 같은 이유로 채울 값이 없으므로 둘 다 nullable로 둔다.
// NULL = 본인 가입, 값 있음 = 그 id_key를 가진 관리자가 추가·수정(AdminMemberCreate/Update).
@AttributeOverrides({
        @AttributeOverride(name = "createdBy", column = @Column(name = "created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by"))
})
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseAuditEntity {

    /**
     * 회원 대리키. 모든 자식 테이블이 참조하는 값.
     *
     * <p>IDENTITY라 Hibernate가 insert 배치를 쓰지 못한다 — 생성된 키를 즉시 받아야
     * 하기 때문이다. 가입 시 기본 지출유형 10종을 만드는 경로(FR-020)가 10번의 개별
     * 왕복이 된다. 현재 규모에서 문제되지 않는다고 보고 유지한다. 바꾸려면 채번 전략
     * 변경이 DDL과 {@code sql/schema-moneylogdb.sql} 덤프까지 함께 가는 변경이므로,
     * 명세 개정 → 스키마 변경 → 덤프 재생성 순서를 밟는다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_key")
    private Long idKey;

    /** 로그인 아이디. 4~20자 영문·숫자·{@code _}. 변경 API가 없어 불변으로 취급한다. */
    @Column(name = "user_id", nullable = false, length = 20, updatable = false)
    private String userId;

    /** bcrypt 해시 문자열(약 60자, 솔트 내장). 평문은 어떤 경우에도 저장하지 않는다. */
    @Column(name = "pw", nullable = false, length = 100)
    private String pw;

    /** 닉네임 2~20자. */
    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    /** 이메일. 비어 있을 수 있고, 값이 있으면 전역 유일({@code ux_user_email}). */
    @Column(name = "email", length = 100)
    private String email;

    /** 전화번호. 하이픈 없이 숫자만. 비어 있을 수 있다. */
    @Column(name = "phone", length = 20)
    private String phone;

    /** 자기소개. 비어 있을 수 있다. */
    @Column(name = "intro", length = 500)
    private String intro;

    /** 권한 값 — 관리자. */
    public static final short ROLE_ADMIN = 1;
    /** 권한 값 — 일반. 가입 기본값이다. */
    public static final short ROLE_MEMBER = 3;
    /** {@link #ROLE_MEMBER}의 DDL 표기. 두 값을 항상 함께 고친다. */
    private static final String ROLE_MEMBER_DDL = "3";

    /**
     * 권한. {@link #ROLE_ADMIN} 또는 {@link #ROLE_MEMBER}만 허용한다.
     *
     * <p>{@code @ColumnDefault}는 DDL용이고 실제 INSERT에 실리는 값은 아래 초기값이다
     * (Hibernate는 {@code @DynamicInsert}가 없으면 컬럼을 항상 INSERT 문에 넣는다).
     * 둘이 어긋나도 경고가 없으므로 한 상수에서 파생시킨다.
     */
    @Column(name = "role", nullable = false)
    @ColumnDefault(ROLE_MEMBER_DDL)
    private Short role = ROLE_MEMBER;

    /** 활성 여부. 관리자 정지 시 {@code false}. 정지해도 회원 행과 가계부 데이터는 남는다. */
    @Column(name = "active", nullable = false)
    @ColumnDefault("true")
    private Boolean active = true;
}
