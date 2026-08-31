package com.dbdomino.moneylog.data.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
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
 * <p>이메일 유일성은 "값이 있을 때만"이라 부분 유니크 인덱스가 필요하다. Hibernate가
 * {@code WHERE} 절 인덱스를 만들지 못하므로 {@code sql/04_constraints.sql}에서
 * {@code ux_user_email}로 건다. {@code role} 값 제한({@code 1} 또는 {@code 3})도
 * 같은 파일의 {@code ck_user_role}이 맡는다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §1</a>
 */
@Entity
@Table(
        name = "tbl_user",
        uniqueConstraints = @UniqueConstraint(name = "ux_user_user_id", columnNames = "user_id")
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

    /** 회원 대리키. 모든 자식 테이블이 참조하는 값. */
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

    /** 권한. {@code 1} 관리자 / {@code 3} 일반. 가입 기본값 {@code 3}. */
    @Column(name = "role", nullable = false)
    @ColumnDefault("3")
    private Short role = 3;

    /** 활성 여부. 관리자 정지 시 {@code false}. 정지해도 회원 행과 가계부 데이터는 남는다. */
    @Column(name = "active", nullable = false)
    @ColumnDefault("true")
    private Boolean active = true;
}
