package com.dbdomino.moneylog.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

/**
 * 회원 세션 — {@code tbl_user_session}.
 *
 * <p>Access·Refresh 토큰을 하나의 세션 행에 함께 담는다. 로그인·재발급·로그아웃이
 * 모두 이 행 하나를 다룬다.
 *
 * <p><b>폐기해도 행을 지우지 않는다</b>(FR-016). 두 해시를 비우고 {@code revoked}만
 * 참으로 바꾼다. 폐기 이력이 남아야 "다른 곳에서 로그인됨"(1006)과 "세션 없음"을
 * 구분할 수 있다. 폐기 행은 무기한 보존한다(FR-019a).
 *
 * <p>회원당 폐기되지 않은 세션은 <b>동시에 1건뿐</b>이다(FR-017). 조건이 붙은
 * 유일성이라 애너테이션으로 표현할 수 없으므로
 * {@link com.dbdomino.moneylog.data.config.MoneylogSchemaContributor} 의 부분
 * 유니크 인덱스 {@code ux_user_session_active}가 강제한다. 애플리케이션 검사만으로는
 * 동시 로그인 레이스를 막을 수 없다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §2</a>
 */
@Entity
@Table(
        name = "tbl_user_session",
        comment = "회원 세션. 회원당 폐기되지 않은 세션은 1건뿐이며, 폐기해도 행은 남는다",
        uniqueConstraints = @UniqueConstraint(name = "ux_user_session_id", columnNames = "session_id")
)
@Getter
@Setter
@NoArgsConstructor
public class UserSession extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    /** 소유 회원. 로그인 아이디가 아니라 대리키({@code id_key})로만 가리킨다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_key",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_session_user")
    )
    private User user;

    /** 세션 식별자. Access Token JWT 클레임 {@code sid}에 담겨 돌아온다. */
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    /** 현재 Access Token 해시. 폐기 시 {@code null}. */
    @Column(name = "access_token_hash", length = 100)
    private String accessTokenHash;

    /** 현재 Refresh Token 해시. 폐기 시 {@code null}. */
    @Column(name = "refresh_token_hash", length = 100)
    private String refreshTokenHash;

    /** Access Token 만료 시각. JWT의 {@code exp}와 함께 둘 다 검증한다. */
    @Column(name = "access_expires_at", nullable = false)
    private OffsetDateTime accessExpiresAt;

    /** Refresh Token 만료 시각. */
    @Column(name = "refresh_expires_at", nullable = false)
    private OffsetDateTime refreshExpiresAt;

    /**
     * JWT 비활성화 여부. 폐기 시 {@code true}(두 해시를 비우는 것과 함께).
     *
     * <p><b>세터를 열지 않는다.</b> 전이 경로를 {@link #revoke()} 하나로 좁힌다.
     * {@code setRevoked(false)} 한 줄로 폐기를 되돌릴 수 있으면, 토큰 해시가 이미
     * 비워진 세션이 "활성"으로 되살아나 부분 유니크 인덱스
     * {@code ux_user_session_active}의 슬롯을 점유한다. 그 회원은 인증에 쓸 수 없는
     * 세션 때문에 정상 로그인이 제약 위반으로 막힌다.
     */
    @Column(name = "revoked", nullable = false)
    @ColumnDefault("false")
    @Setter(AccessLevel.NONE)
    private Boolean revoked = false;

    /** 마지막 API 인증 성공 시각. 선택 항목이라 비어 있을 수 있다. */
    @Column(name = "last_accessed_at")
    private OffsetDateTime lastAccessedAt;

    /**
     * 세션을 폐기한다 — 두 해시를 비우고 {@code revoked}를 참으로 바꾼다.
     *
     * <p>행을 지우지 않는 것이 핵심이다(FR-016). 로그아웃·중복 로그인·비밀번호
     * 변경·관리자 정지가 모두 이 절차를 쓴다.
     */
    public void revoke() {
        this.accessTokenHash = null;
        this.refreshTokenHash = null;
        this.revoked = true;
    }
}
